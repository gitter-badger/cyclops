package com.aol.cyclops.lambda.api;

import com.nurkiewicz.lazyseq.LazySeq;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectionCache {
	private final static Map<Class,List<Field>> fields = new ConcurrentHashMap<>();

	private final static Map<Class,Optional<Method>> unapplyMethods =new ConcurrentHashMap<>();
	public static List<Field> getField(
			Class class1) {
		return fields.computeIfAbsent(class1, cl ->{
			return LazySeq.iterate(class1, c->c.getSuperclass())
						.takeWhile(c->c!=Object.class)
						.flatMap(c->LazySeq.of(c.getDeclaredFields()))
						.filter(f->!Modifier.isStatic(f.getModifiers()))
						.map(f -> { f.setAccessible(true); return f;})
						.toList();
					});
		
	}
	
	public static Optional<Method> getUnapplyMethod(Class c) {
	
			return unapplyMethods.computeIfAbsent(c, cl -> {
				try{
					return Optional.of(cl.getMethod("unapply"));
				}catch(NoSuchMethodException e){
					return Optional.empty();
				}
			});	
		
	}
	
	
}