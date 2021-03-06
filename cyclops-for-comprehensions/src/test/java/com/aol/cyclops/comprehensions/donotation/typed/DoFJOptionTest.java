package com.aol.cyclops.comprehensions.donotation.typed;


import static com.aol.cyclops.lambda.api.AsAnyM.notTypeSafeAnyM;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.function.BiFunction;

import org.junit.Ignore;
import org.junit.Test;

import com.aol.cyclops.comprehensions.donotation.UntypedDo;
import com.aol.cyclops.lambda.api.AsAnyM;
import com.aol.cyclops.lambda.monads.AnyM;

import fj.data.Either;
import fj.data.Option;

public class DoFJOptionTest {
	@Test
	public void optionTest(){
		AnyM<Integer> one = notTypeSafeAnyM(Option.some(1));
		AnyM<Integer> empty = notTypeSafeAnyM(Option.none());
		BiFunction<Integer,Integer,Integer> f2 = (a,b) -> a *b; 
		
		Option result =  Do.add(one)
							.add(empty)
							.yield(  a -> b -> f2.apply(a,b)).unwrap();
		
		System.out.println(result);
		assertTrue(result.isNone());

	}
	@Test
	public void optionTestWith(){
		AnyM<Integer> one = notTypeSafeAnyM(Option.some(1));
		AnyM<Integer> empty = notTypeSafeAnyM(Option.none());
		BiFunction<Integer,Integer,Integer> f2 = (a,b) -> a *b; 
		
		Option result =  Do.add(one)
							.withAnyM( i-> empty)
							.yield(  a -> b -> f2.apply(a,b)).unwrap();
		
		System.out.println(result);
		assertTrue(result.isNone());

	}
	@Test
	public void optionPositiveTest(){
		AnyM<Integer> one = notTypeSafeAnyM(Option.some(1));
		AnyM<Integer> empty = notTypeSafeAnyM(Option.some(3));
		BiFunction<Integer,Integer,Integer> f2 = (a,b) -> a *b; 
		
		Option result =  Do.add(one)
							.add(empty)
							.yield(  a -> b -> f2.apply(a,b)).unwrap();
		
		System.out.println(result);
		assertEquals(result.some(),3);

	}
	
}
