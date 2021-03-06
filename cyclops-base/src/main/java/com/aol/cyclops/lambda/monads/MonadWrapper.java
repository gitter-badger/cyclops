package com.aol.cyclops.lambda.monads;

import lombok.Value;
import lombok.experimental.Wither;

import com.aol.cyclops.internal.Monad;
import com.aol.cyclops.lambda.api.Decomposable;

@Value
public class MonadWrapper<MONAD,T> implements Monad<MONAD,T>, Decomposable{
	@Wither
	private final Object monad;
	public static <MONAD,T> Monad<MONAD,T>  of(Object of) {
		return new MonadWrapper(of);
		
	}
	public MONAD unwrap(){
		return (MONAD)monad;
	}
	@Override
	public <X> AnyM<X> anyM(){
		return new AnyM<X>((Monad)this);	
	}
	@Override
	public SequenceM<T>  sequence(){
		return new  SequenceM(this.stream());	
	}
}
