package com.aol.cyclops.featuretoggle;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.aol.cyclops.lambda.api.Streamable;
import com.aol.cyclops.lambda.monads.AnyM;
import com.aol.cyclops.value.ValueObject;



/**
 * Switch interface for handling features that can be enabled or disabled.
 * 
 * @author johnmcclean
 *
 * @param <F>
 */
public interface FeatureToggle<F> extends Supplier<F>, ValueObject, Streamable<F> {

	boolean isEnabled();
	boolean isDisabled();
	
	/**
	 * @return This monad, wrapped as AnyM
	 */
	public AnyM<F> anyM();
	/**
	 * @return This monad, wrapped as AnyM of Disabled
	 */
	public AnyM<F> anyMDisabled();
	/**
	 * @return This monad, wrapped as AnyM of Enabled
	 */
	public AnyM<F> anyMEnabled();
	F get();
	
	default <T extends Iterable<?>> T unapply(){
		return (T)Arrays.asList(get());
	}
	/**
	 * Create a new enabled switch
	 * 
	 * @param f switch value
	 * @return enabled switch
	 */
	public static <F> Enabled<F> enable(F f){
		return new Enabled<F>(f);
	}
	/**
	 * Create a new disabled switch
	 * 
	 * @param f switch value
	 * @return disabled switch
	 */
	public static <F> Disabled<F> disable(F f){
		return new Disabled<F>(f);
	}
	
	/**
	 * 
	 * 
	 * @param from Create a switch with the same type
	 * @param f but with this value (f)
	 * @return new switch
	 */
	public static <F> FeatureToggle<F> from(FeatureToggle<F> from, F f){
		if(from.isEnabled())
			return enable(f);
		return disable(f);
	}
	
	/**
	 * Flatten a nested Switch, maintaining top level enabled / disabled semantics
	 * 
	 * <pre>
	 * Enabled&lt;Enabled&lt;Disabled&gt;&gt; nested= Switch.enable(Switch.enable(Switch.disable(100)));
	 * </pre>
	 * 
	 * unwraps to enabled[100]
	 * 
	 * @return flattened switch
	 */
	default <X> FeatureToggle<X> flatten(){
		
		Optional s = Optional.of(get()).flatMap(x->{
			if(x instanceof FeatureToggle)
				return Optional.of(((FeatureToggle)x).flatten());
			else
				return Optional.of(FeatureToggle.from(this,x));
		});
		Object value = s.get();
		FeatureToggle<X> newSwitch = from((FeatureToggle<X>)this,((FeatureToggle<X>)value).get());
		return newSwitch;
	}
	
	/**
	 * Peek at current switch value
	 * 
	 * @param consumer Consumer to provide current value to
	 * @return This Switch
	 */
	default FeatureToggle<F> peek(Consumer<F> consumer){
		consumer.accept(get());
		return this;
	}
	
	/**
	 * @param map Create a new Switch with provided function
	 * @return switch from function
	 */
	default <X> FeatureToggle<X> flatMap(Function<F,FeatureToggle<X>> map){
		if(isDisabled())
			return (FeatureToggle<X>)this;
		return map.apply(get());
	}
	
	
	
	/**
	 * @param map transform the value inside this Switch into new Switch object
	 * @return new Switch with transformed value
	 */
	default <X> FeatureToggle<X> map(Function<F,X> map){
		if(isDisabled())
			return (FeatureToggle<X>)this;
		return enable(map.apply(get()));
	}
	
	/**
	 * Filter this Switch. If current value does not meet criteria,
	 * a disabled Switch is returned
	 * 
	 * @param p Predicate to test for
	 * @return Filtered switch
	 */
	default FeatureToggle<F> filter(Predicate<F> p){
		if(isDisabled())
			return this;
		if(!p.test(get()))
			return FeatureToggle.disable(get());
		return this;
	}
	
	/**
	 * Iterate over value in switch (single value, so one iteration)
	 * @param consumer to provide value to.
	 */
	default void forEach(Consumer<? super F> consumer){
		if(isDisabled())
			return;
		consumer.accept(get());
	}
	/**
	 * @return transform this Switch into an enabled Switch
	 */
	default Enabled<F> enable(){
		return new Enabled<F>(get()); 
	}
	/**
	 * @return transform this Switch into a disabled Switch
	 */
	default Disabled<F> disable(){
		return new Disabled<F>(get()); 
	}
	/**
	 * @return flip this Switch
	 */
	default FeatureToggle<F> flip(){
		
		if(isEnabled())
			return disable(get());
		else
			return enable(get());
	}
	
	
	/**
	 * @return Optional.empty() if disabled, Optional containing current value if enabled
	 */
	default Optional<F> optional(){
		return stream().findFirst();	
	}
	
	/**
	 * @return emty Stream if disabled, Stream with current value if enabled.
	 */
	@Override
	default Stream<F> stream(){
		if(isEnabled())
			return Stream.of(get());
		else
			return Stream.of();
	}
	
	
}
