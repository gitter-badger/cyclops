package com.aol.cyclops.comprehensions.donotation.typed;


import java.util.function.Function;

import lombok.AllArgsConstructor;

import org.pcollections.PStack;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import com.aol.cyclops.comprehensions.ComprehensionData;
import com.aol.cyclops.comprehensions.ForComprehensions;
import com.aol.cyclops.lambda.api.Unwrapable;
import com.aol.cyclops.lambda.utils.Mutable;

@AllArgsConstructor
public abstract class DoComp {
	
	PStack<Entry> assigned;
	
	protected PStack<Entry> addToAssigned(Function f){
		return assigned.plus(assigned.size(),createEntry(f));
	}
	protected Entry createEntry(Function f){
		return new Entry("$$monad"+assigned.size(),new Assignment(f));
	}
	
	protected <T> T yieldInternal(Function f){
		return (T)ForComprehensions.foreachX(c->build(c,f));
	}
	
	 @SuppressWarnings({"rawtypes","unchecked"})
	private Object handleNext(Entry e,ComprehensionData c,PVector<String> newList){
		
		if(e.getValue() instanceof Guard){
			
			final Function f = ((Guard)e.getValue()).getF();
			c.filter( ()-> {
						
						return unwrapNestedFunction(c, f, newList);
							
							}  );
			
		}
		else if(e.getValue() instanceof Assignment){
			
			final Function f = ((Assignment)e.getValue()).getF();
			c.$(e.getKey(), ()-> {
							
								return unwrapNestedFunction(c, f, newList);
							
							}  );
			
		}
		else
			c.$(e.getKey(),handleUnwrappable(e.getValue()));
		
		return null;
	}
	 private Object handleUnwrappable(Object o){
		 if(o instanceof Unwrapable)
				return ((Unwrapable)o).unwrap();
			return o;
	 }
	private Object build(
			ComprehensionData c, Function f) {
		Mutable<PVector<String>> vars = new Mutable<>(TreePVector.empty());
		assigned.stream().forEach(e-> addToVar(e,vars,handleNext(e,c,vars.get())));
		Mutable<Object> var = new Mutable<>(f);
		
		return c.yield(()-> { 
			return unwrapNestedFunction(c, f, vars.get());
			
	}  );
		
	}
	private Object unwrapNestedFunction(ComprehensionData c, Function f,
			PVector<String> vars) {
		Function next = f;
		Object result = null;
		for(String e : vars){
			
				result = next.apply(c.$(e ));
			if(result instanceof Function){
				next = ((Function)result);
			}
			
		}
		if(result instanceof Unwrapable)
			return ((Unwrapable)result).unwrap();
		return result;
	}

	private Object addToVar(Entry e,Mutable<PVector<String>> vars, Object handleNext) {
		if(!(e.getValue() instanceof Guard)){	
			PVector<String> vector = vars.get();
			vars.set(vector.plus(vector.size(),e.getKey()));
		}
		return handleNext;
	}

}