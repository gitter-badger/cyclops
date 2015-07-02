package com.aol.cyclops.lambda.monads;

import static com.aol.cyclops.internal.AsGenericMonad.monad;
import static com.aol.cyclops.lambda.api.AsAnyM.anyM;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.BaseStream;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import com.aol.cyclops.internal.AsGenericMonad;
import com.aol.cyclops.internal.Monad;
import com.aol.cyclops.lambda.api.AsStreamable;
import com.aol.cyclops.lambda.api.Monoid;
import com.aol.cyclops.lambda.api.Streamable;
import com.aol.cyclops.lambda.api.Unwrapable;
import com.aol.cyclops.streams.StreamUtils;
import com.nurkiewicz.lazyseq.LazySeq;


public class SequenceM<T> implements Unwrapable {
	private final LazySeq<T> monad;
	SequenceM(LazySeq<T> seq){
		monad = seq;
	}
	SequenceM(Stream<T> stream){
		monad = LazySeq.of(stream.iterator());
	}
	static <T> SequenceM<T> monad(Stream<T> stream){
		return new SequenceM(stream);
	}
	static <T> SequenceM<T> monad(LazySeq<T> stream){
		return new SequenceM(stream);
	}
	public final <R> R unwrap(){
		return (R)monad;
	}
	/**
	 * Type safe unwrap
	 * 
	 * @return Stream with current wrapped values
	 */
	public final Stream<T> unwrapStream(){
		
		Stream unwrapper = Stream.of(1);
		return (Stream)new ComprehenderSelector()
							.selectComprehender(unwrapper)
							.executeflatMap(unwrapper, i-> unwrap());
		
	}
	public final Optional<List<T>> unwrapOptional(){
		
		Optional unwrapper = Optional.of(1);
		return (Optional)new ComprehenderSelector()
							.selectComprehender(unwrapper)
							.executeflatMap(unwrapper, i-> unwrap());
		
	}
	public final CompletableFuture<List<T>> unwrapCompletableFuture(){
		
		CompletableFuture unwrapper = CompletableFuture.completedFuture(1);
		return (CompletableFuture)new ComprehenderSelector()
							.selectComprehender(unwrapper)
							.executeflatMap(unwrapper, i-> unwrap());
		
	}
	
	/**
	 * Convert to a Stream with the values repeated specified times
	 * 
	 * @param times
	 *            Times values should be repeated within a Stream
	 * @return Stream with values repeated
	 */
	public final SequenceM<T> cycle(int times) {
		return new SequenceM(StreamUtils.cycle(times,AsStreamable.asStreamable(monad.stream())));
	}

	/**
	 * Convert to a Stream with the result of a reduction operation repeated
	 * specified times
	 * 
	 * <pre>
	 * {@code 
	 *   		List<Integer> list = AsGenericMonad,asMonad(Stream.of(1,2,2))
	 * 										.cycle(Reducers.toCountInt(),3)
	 * 										.collect(Collectors.toList());
	 * 	//is asList(3,3,3);
	 *   }
	 * </pre>
	 * 
	 * @param m
	 *            Monoid to be used in reduction
	 * @param times
	 *            Number of times value should be repeated
	 * @return Stream with reduced values repeated
	 */
	public final SequenceM<T> cycle(Monoid<T> m, int times) {
		return monad(StreamUtils.cycle(times,AsStreamable.asStreamable(m.reduce(createLazySeq().stream()))));
		
	}

	private LazySeq<T> createLazySeq() {
		return monad;
	}
	/**
	 * 
	 * Convert to a Stream, repeating the resulting structure specified times
	 * and lifting all values to the specified Monad type
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;Optional&lt;Integer&gt;&gt; list = monad(Stream.of(1, 2)).cycle(Optional.class,
	 * 			2).toList();
	 * 
	 * 	// is asList(Optional.of(1),Optional.of(2),Optional.of(1),Optional.of(2) ));
	 * 
	 * }
	 * </pre>
	 * 
	 * 
	 * 
	 * @param monadC
	 *            class type
	 * @param times
	 * @return
	 */
	public final <R> SequenceM<R> cycle(Class<R> monadC, int times) {
		return (SequenceM)cycle(times).map(r -> new ComprehenderSelector().selectComprehender(monad).of(r));	
	}

	/**
	 * Repeat in a Stream while specified predicate holds
	 * 
	 * @param predicate
	 *            repeat while true
	 * @return Repeating Stream
	 */
	public final SequenceM<T> cycleWhile(Predicate<? super T> predicate) {
		return monad(LazySeq.of(StreamUtils.cycle(createLazySeq().stream()).iterator()).takeWhile(predicate).stream());
	}

	/**
	 * Repeat in a Stream until specified predicate holds
	 * 
	 * @param predicate
	 *            repeat while true
	 * @return Repeating Stream
	 */
	public final SequenceM<T> cycleUntil(Predicate<? super T> predicate) {
		return monad(LazySeq.of(StreamUtils.cycle(createLazySeq().stream()).iterator()).takeWhile(predicate.negate()).stream());
	}

	/**
	 * Generic zip function. E.g. Zipping a Stream and an Optional
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	Stream&lt;List&lt;Integer&gt;&gt; zipped = asMonad(Stream.of(1, 2, 3)).zip(
	 * 			asMonad(Optional.of(2)), (a, b) -&gt; Arrays.asList(a, b));
	 * 	// [[1,2]]
	 * }
	 * </pre>
	 * 
	 * @param second
	 *            Monad to zip with
	 * @param zipper
	 *            Zipping function
	 * @return Stream zipping two Monads
	 */
	public final <S, R> SequenceM<R> zip(SequenceM<? extends S> second,
			BiFunction<? super T, ? super S, ? extends R> zipper) {
		return monad((Stream)LazySeq.of(createLazySeq().iterator()).zip(LazySeq.of(second.createLazySeq().iterator()), zipper).stream());
		
	}
	public final <S, R> SequenceM<R> zip(AnyM<? extends S> second,
			BiFunction<? super T, ? super S, ? extends R> zipper) {
		return zip(second.toSequence(), zipper);
	}

	/**
	 * Zip this Monad with a Stream
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	Stream&lt;List&lt;Integer&gt;&gt; zipped = asMonad(Stream.of(1, 2, 3)).zip(
	 * 			Stream.of(2, 3, 4), (a, b) -&gt; Arrays.asList(a, b));
	 * 
	 * 	// [[1,2][2,3][3,4]]
	 * }
	 * </pre>
	 * 
	 * @param second
	 *            Stream to zip with
	 * @param zipper
	 *            Zip funciton
	 * @return This monad zipped with a Stream
	 */
	public final <S, R> SequenceM<R> zip(Stream<? extends S> second,
			BiFunction<? super T, ? super S, ? extends R> zipper) {
		return monad((Stream)LazySeq.of(createLazySeq().iterator()).zip(LazySeq.of(second.iterator()), zipper).stream());
	}

	/**
	 * Create a sliding view over this monad
	 * 
	 * @param windowSize
	 *            Size of sliding window
	 * @return Stream with sliding view over monad
	 */
	public final SequenceM<List<T>> sliding(int windowSize) {
		return monad(createLazySeq().sliding(windowSize).stream());
	}

	/**
	 * Group elements in a Monad into a Stream
	 * 
	 * <pre>
	 * {
	 * 	&#064;code
	 * 	List&lt;List&lt;Integer&gt;&gt; list = monad(Stream.of(1, 2, 3, 4, 5, 6)).grouped(3)
	 * 			.collect(Collectors.toList());
	 * 
	 * 	assertThat(list.get(0), hasItems(1, 2, 3));
	 * 	assertThat(list.get(1), hasItems(4, 5, 6));
	 * 
	 * }
	 * </pre>
	 * 
	 * @param groupSize
	 *            Size of each Group
	 * @return Stream with elements grouped by size
	 */
	public final SequenceM<List<T>> grouped(int groupSize) {
		return monad(LazySeq.of(createLazySeq().iterator()).grouped(groupSize).stream());
	}

	/*
	 * Return the distinct Stream of elements
	 * 
	 * <pre>{@code List<Integer> list =
	 * monad(Optional.of(Arrays.asList(1,2,2,2,5,6)))
	 * .<Stream<Integer>,Integer>streamedMonad() .distinct()
	 * .collect(Collectors.toList()); }</pre>
	 */
	public final SequenceM<T> distinct() {
		return monad(monad.distinct());
	}

	/**
	 * Scan left using supplied Monoid
	 * 
	 * <pre>
	 * {@code  
	 * 
	 * 	assertEquals(asList("", "a", "ab", "abc"),monad(Stream.of("a", "b", "c")).scanLeft(Reducers.toString("")).toList());
	 *         
	 *         }
	 * </pre>
	 * 
	 * @param monoid
	 * @return
	 */
	public final SequenceM<T> scanLeft(Monoid<T> monoid) {
		return monad(monad.scan(monoid.zero(), monoid.reducer()).stream());
	}

	
	/**
	 * @return Monad converted to Stream via stream() and sorted - to access
	 *         nested collections in non-Stream monads as a stream use
	 *         streamedMonad() first
	 * 
	 *         e.g.
	 * 
	 *         <pre>
	 * {@code 
	 *    monad(Optional.of(Arrays.asList(1,2,3))).sorted()  // Monad[Stream[List[1,2,3]]]
	 *    
	 *     monad(Optional.of(Arrays.asList(1,2,3))).streamedMonad().sorted() // Monad[Stream[1,2,3]]
	 *  }
	 * </pre>
	 * 
	 *         <pre>
	 * {@code assertThat(monad(Stream.of(4,3,6,7)).sorted().toList(),equalTo(Arrays.asList(3,4,6,7))); }
	 * </pre>
	 * 
	 */
	public final SequenceM<T> sorted() {
		return monad(monad.sorted());
	}

	/**
	 *
	 * 
	 * e.g.
	 * 
	 * <pre>
	 * {@code 
	 *    anyM(Optional.of(Arrays.asList(1,2,3))).asSequence().sorted( (a,b)->b-a)  // Monad[Stream[List[1,2,3]]]
	 *    
	 *     anyM(Optional.of(Arrays.asList(1,2,3))).toSequence().sorted( (a,b)->b-a) // Monad[Stream[3,2,1]]
	 *  }
	 * </pre>
	 * 
	 * 
	 * 
	 * @param c
	 *            Compartor to sort with
	 * @return Sorted Monad
	 */
	public final SequenceM<T> sorted(Comparator<? super T> c) {
		return monad(monad.sorted(c));
	}

	/**
	 * <pre>
	 * {@code assertThat(anyM(Stream.of(4,3,6,7)).asSequence().skip(2).toList(),equalTo(Arrays.asList(6,7))); }
	 * </pre>
	 * 
	
	 * 
	 * @param num
	 *            Number of elemenets to skip
	 * @return Monad converted to Stream with specified number of elements
	 *         skipped
	 */
	public final SequenceM<T> skip(int num) {
		return monad(monad.drop(num));
	}

	/**
	 * 
	 * 
	 * <pre>
	 * {@code
	 * assertThat(anyM(Stream.of(4,3,6,7)).asSequence().sorted().skipWhile(i->i<6).toList(),equalTo(Arrays.asList(6,7)));
	 * }
	 * </pre>
	 * 
	 * @param p
	 *            Predicate to skip while true
	 * @return Monad converted to Stream with elements skipped while predicate
	 *         holds
	 */
	public final SequenceM<T> skipWhile(Predicate<? super T> p) {
		return monad(monad.dropWhile(p));
	}

	/**
	 * 
	 * 
	 * <pre>
	 * {@code assertThat(anyM(Stream.of(4,3,6,7)).asSequence().skipUntil(i->i==6).toList(),equalTo(Arrays.asList(6,7)));}
	 * </pre>
	 * 
	 * 
	 * @param p
	 *            Predicate to skip until true
	 * @return Monad converted to Stream with elements skipped until predicate
	 *         holds
	 */
	public final SequenceM<T> skipUntil(Predicate<? super T> p) {
		return monad(monad.dropWhile(p.negate()));
	}

	/**
	 * 
	 * 
	 * <pre>
	 * {@code assertThat(anyM(Stream.of(4,3,6,7)).asSequence().limit(2).toList(),equalTo(Arrays.asList(4,3)));}
	 * </pre>
	 * 
	 * @param num
	 *            Limit element size to num
	 * @return Monad converted to Stream with elements up to num
	 */
	public final SequenceM<T> limit(int num) {
		return monad(monad.limit(num));
	}

	/**
	 * NB to access nested collections in non-Stream monads as a stream use
	 * streamedMonad() first
	 * 
	 * <pre>
	 * {@code assertThat(anyM(Stream.of(4,3,6,7)).asSequence().sorted().limitWhile(i->i<6).toList(),equalTo(Arrays.asList(3,4)));}
	 * </pre>
	 * 
	 * @param p
	 *            Limit while predicate is true
	 * @return Monad converted to Stream with limited elements
	 */
	public final SequenceM<T> limitWhile(Predicate<? super T> p) {
		return monad(monad.takeWhile(p));
	}

	/**
	 * NB to access nested collections in non-Stream monads as a stream use
	 * streamedMonad() first
	 * 
	 * <pre>
	 * {@code assertThat(anyM(Stream.of(4,3,6,7)).limitUntil(i->i==6).toList(),equalTo(Arrays.asList(4,3))); }
	 * </pre>
	 * 
	 * @param p
	 *            Limit until predicate is true
	 * @return Monad converted to Stream with limited elements
	 */
	public final SequenceM<T> limitUntil(Predicate<? super T> p) {
		return monad(monad.takeWhile(p.negate()));
	}
	/**
	 * @return this monad converted to a Parallel Stream, via streamedMonad() wraped in Monad interface
	 */
	public final <NT> SequenceM<NT> parallel(){
		return null;//monad.parallel().sequence();
	}
	
	/**
	 * True if predicate matches all elements when Monad converted to a Stream
	 * 
	 * @param c Predicate to check if all match
	 */
	public final  boolean  allMatch(Predicate<? super T> c) {
		return monad.allMatch(c);
	}
	/**
	 * True if a single element matches when Monad converted to a Stream
	 * 
	 * @param c Predicate to check if any match
	 */
	public final  boolean  anyMatch(Predicate<? super T> c) {
		return monad.stream().anyMatch(c);
	}
	public  boolean xMatch(int num, Predicate<T> c) {
		return monad.stream().map(t -> c.test(t)) .collect(Collectors.counting()) == num;
	}
	public final  boolean  noneMatch(Predicate<T> c) {
		return monad.allMatch(c.negate());
	}
	public final  String mkString(String sep){
		return monad.mkString(sep);
	}
	public final   String mkString(String start, String sep,String end){
		return monad.mkString(start, sep, end);
	}
	public final   String mkString(String start, String sep,String end,boolean lazy){
		return monad.mkString(start, sep, end, lazy);
		
	}
	
	public final  <C extends Comparable<? super C>> Optional<T> minBy(Function<T,C> f){
		return monad.minBy(f);
	}
	public final   Optional<T> min(Comparator<? super T> comparator){
		return monad.min(comparator);
	}
	public final  <C extends Comparable<? super C>> Optional<T> maxBy(Function<T,C> f){
		
		return monad.maxBy(f);
	}
	public final  Optional<T> max(Comparator<? super T> comparator){
		return monad.max(comparator);
	}
	
	
	
	public final  T head(){
		return stream().head();
	}
	public final  SequenceM<T> tail(){
		
		return new SequenceM(monad.tail());
	}
	/**
	 * @return First matching element in sequential order
	 * 
	 * (deterministic)
	 * 
	 */
	public final  Optional<T>  findFirst() {
		return stream().stream().findFirst();
	}
	/**
	 * @return first matching element,  but order is not guaranteed
	 * 
	 * (non-deterministic) 
	 */
	public final  Optional<T>  findAny() {
		return stream().stream().findAny();
	}
	
	/**
	 * Attempt to map this Monad to the same type as the supplied Monoid (using mapToType on the monoid interface)
	 * Then use Monoid to reduce values
	 * 
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	public final  <R> R mapReduce(Monoid<R> reducer){
		return reducer.mapReduce(stream().stream());
	}
	/**
	 *  Attempt to map this Monad to the same type as the supplied Monoid, using supplied function
	 *  Then use Monoid to reduce values
	 *  
	 * @param mapper Function to map Monad type
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	public final  <R> R mapReduce(Function<? super T,? extends R> mapper, Monoid<R> reducer){
		return reducer.reduce(stream().stream().map(mapper));
	}
	
	/**
	 * Mutable reduction / collection over this Monad converted to a Stream
	 * 
	 * @param collector Collection operation definition
	 * @return Collected result
	 */
	public final <R, A> R collect(Collector<? super T, A, R> collector){
		return stream().stream().collect(collector);
	}
	/**
	 * Apply multiple collectors Simulataneously to this Monad
	 * 
	 * <pre>{@code
	  	List result = monad(Stream.of(1,2,3)).collect(Stream.of(Collectors.toList(),
	  															Collectors.summingInt(Integer::intValue),
	  															Collectors.averagingInt(Integer::intValue)));
		
		assertThat(result.get(0),equalTo(Arrays.asList(1,2,3)));
		assertThat(result.get(1),equalTo(6));
		assertThat(result.get(2),equalTo(2.0));
		}</pre>
		
		 * NB if this Monad is an Optional [Arrays.asList(1,2,3)]  reduce will operate on the Optional as if the list was one value
	 * To reduce over the values on the list, called streamedMonad() first. I.e. streamedMonad().collect(collectors);
	 * 
	 * @param collectors Stream of Collectors to apply
	 * @return  List of results
	 */
	public final  List collect(Stream<Collector> collectors){
		return StreamUtils.collect(stream().stream(),collectors);
	}
	
	/**
	 * NB if this Monad is an Optional [Arrays.asList(1,2,3)]  reduce will operate on the Optional as if the list was one value
	 * To reduce over the values on the list, called streamedMonad() first. I.e. streamedMonad().reduce(reducer)
	 * 
	 * 
	 * @param reducer Use supplied Monoid to reduce values
	 * @return reduced values
	 */
	public final  T reduce(Monoid<T> reducer){
		return reducer.reduce(stream().stream());
	}
	
	/**
     * Reduce with multiple reducers in parallel
	 * NB if this Monad is an Optional [Arrays.asList(1,2,3)]  reduce will operate on the Optional as if the list was one value
	 * To reduce over the values on the list, called streamedMonad() first. I.e. streamedMonad().reduce(reducer)
	 * 
	 * @param reducers
	 * @return
	 */
	public final List<T> reduce(Stream<Monoid<T>> reducers){
		return StreamUtils.reduce(stream().stream(), reducers);
	}
	/**
     * Reduce with multiple reducers in parallel
	 * NB if this Monad is an Optional [Arrays.asList(1,2,3)]  reduce will operate on the Optional as if the list was one value
	 * To reduce over the values on the list, called streamedMonad() first. I.e. streamedMonad().reduce(reducer)
	 * 
	 * @param reducers
	 * @return
	 */
	public final List<T> reduce(Iterable<Monoid<T>> reducers){
		return StreamUtils.reduce(stream().stream(), reducers);
	}
	
	/**
	 * 
	 * 
	 * @param reducer Use supplied Monoid to reduce values starting via foldLeft
	 * @return Reduced result
	 */
	public final T foldLeft(Monoid<T> reducer){
		return reduce(reducer);
	}
	/**
	 *  Attempt to map this Monad to the same type as the supplied Monoid (using mapToType on the monoid interface)
	 * Then use Monoid to reduce values
	 * 
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	public final <T> T foldLeftMapToType(Monoid<T> reducer){
		return reducer.mapReduce(stream().stream());
	}
	/**
	 * 
	 * 
	 * @param reducer Use supplied Monoid to reduce values starting via foldRight
	 * @return Reduced result
	 */
	public final T foldRight(Monoid<T> reducer){
		return reducer.reduce(StreamUtils.reverse(stream().stream()));
	}
	/**
	 *  Attempt to map this Monad to the same type as the supplied Monoid (using mapToType on the monoid interface)
	 * Then use Monoid to reduce values
	 * 
	 * @param reducer Monoid to reduce values
	 * @return Reduce result
	 */
	public final <T> T foldRightMapToType(Monoid<T> reducer){
		return reducer.mapReduce(StreamUtils.reverse(stream().stream()));
	}
	/**
	 * @return Underlying monad converted to a Streamable instance
	 */
	public final Streamable<T> toStreamable(){
		return  AsStreamable.asStreamable(stream());
	}
	/**
	 * @return This monad converted to a set
	 */
	public final Set<T> toSet(){
		return (Set)stream().stream().collect(Collectors.toSet());
	}
	/**
	 * @return this monad converted to a list
	 */
	public final List<T> toList(){
		return (List)stream().stream().collect(Collectors.toList());
	}
	/**
	 * @return  calls to stream() but more flexible on type for inferencing purposes.
	 */
	public final <T> Stream<T> toStream(){
		return (Stream<T>) stream().stream();
	}
	/**
	 * Unwrap this Monad into a Stream.
	 * If the underlying monad is a Stream it is returned
	 * Otherwise we flatMap the underlying monad to a Stream type
	 */
	public final LazySeq<T> stream(){
		 if(unwrap() instanceof LazySeq)
			return (LazySeq)unwrap();
		if(unwrap() instanceof Iterable)
			return LazySeq.of((Iterable)unwrap());
		
		return monad;
	//	return monad.lazySeq();
			
	}
	
	/**
	 * 
	 * <pre>{@code 
	 * assertTrue(monad(Stream.of(1,2,3,4)).startsWith(Arrays.asList(1,2,3)));
	 * }</pre>
	 * 
	 * @param iterable
	 * @return True if Monad starts with Iterable sequence of data
	 */
	public final boolean startsWith(Iterable<T> iterable){
		return monad.startsWith(iterable);
		
	}
	/**
	 * 	<pre>{@code assertTrue(monad(Stream.of(1,2,3,4)).startsWith(Arrays.asList(1,2,3).iterator())) }</pre>

	 * @param iterator
	 * @return True if Monad starts with Iterators sequence of data
	 */
	public final boolean startsWith(Iterator<T> iterator){
		return monad.startsWith(iterator);
		
	}
	
	public AnyM<T> anyM(){
		return new AnyM<>(AsGenericMonad.asMonad(monad));
	}
	public final  <R> SequenceM<R> map(Function<? super T,? extends R> fn){
		return new SequenceM(monad.map(fn));
	}
	public final   SequenceM<T>  peek(Consumer<? super T> c) {
		return new SequenceM(monad.stream().peek(c));
	}
	/**
	 * flatMap operation
	 * 
	 * @param fn
	 * @return
	 */
	public final <R> SequenceM<R> flatMap(Function<? super T,SequenceM<? extends R>> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).bind(in -> fn.apply(in).unwrap()).sequence();
	}
	public final <R> SequenceM<R> flatMapAnyM(Function<? super T,AnyM<? extends R>> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).bind(in -> fn.apply(in).unwrap()).sequence();
	}
	/**
	 * Convenience method & performance optimisation
	 * 
	 * flatMapping to a Stream will result in the Stream being converted to a List, if the host Monad
	 * type is not a Stream. (i.e.
	 *  <pre>
	 *  {@code  
	 *   AnyM<Integer> opt = anyM(Optional.of(20));
	 *   Optional<List<Integer>> optionalList = opt.flatMap( i -> anyM(Stream.of(1,2,i))).unwrap();  
	 *   
	 *   //Optional [1,2,20]
	 *  }</pre>
	 *  
	 *  In such cases using Arrays.asList would be more performant
	 *  <pre>
	 *  {@code  
	 *   AnyM<Integer> opt = anyM(Optional.of(20));
	 *   Optional<List<Integer>> optionalList = opt.flatMapCollection( i -> asList(1,2,i))).unwrap();  
	 *   
	 *   //Optional [1,2,20]
	 *  }</pre>
	 * @param fn
	 * @return
	 */
	public final <R> SequenceM<R> flatMapCollection(Function<? super T,Collection<? extends R>> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).bind(in -> fn.apply(in)).sequence();
	}
	public final <R> SequenceM<R> flatMapStream(Function<? super T,BaseStream<? extends R,?>> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad( monad).bind(in -> fn.apply(in)).sequence();
	}
	public final <R> SequenceM<R> flatMapOptional(Function<? super T,Optional<? extends R>> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).bind(in -> fn.apply(in)).sequence();
	}
	public final <R> SequenceM<R> flatMapCompletableFuture(Function<? super T,CompletableFuture<? extends R>> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).bind(in -> fn.apply(in)).sequence();
	}
	public final <R> SequenceM<R> flatMapLazySeq(Function<? super T,LazySeq<? extends R>> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).bind(in -> fn.apply(in)).sequence();
	}
	
	/**
	 * Perform a flatMap operation where the result will be a flattened stream of Characters
	 * from the CharSequence returned by the supplied function.
	 * 
	 * <pre>
	 * {@code 
	 *   List<Character> result = anyM("input.file")
									.asSequence()
									.liftAndBindCharSequence(i->"hello world")
									.toList();
		
		assertThat(result,equalTo(Arrays.asList('h','e','l','l','o',' ','w','o','r','l','d')));
	 * }
	 * </pre>
	 * 
	 * @param fn
	 * @return
	 */
	public final  SequenceM<Character> liftAndBindCharSequence(Function<? super T,CharSequence> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).liftAndBind(fn).sequence();
	}
	/**
	 *  Perform a flatMap operation where the result will be a flattened stream of Strings
	 * from the text loaded from the supplied files.
	 * 
	 * <pre>
	 * {@code
	 * 
		List<String> result = anyM("input.file")
								.asSequence()
								.map(getClass().getClassLoader()::getResource)
								.peek(System.out::println)
								.map(URL::getFile)
								.liftAndBindFile(File::new)
								.toList();
		
		assertThat(result,equalTo(Arrays.asList("hello","world")));
	 * 
	 * }
	 * 
	 * </pre>
	 * 
	 * @param fn
	 * @return
	 */
	public final  SequenceM<String> liftAndBindFile(Function<? super T,File> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).liftAndBind(fn).sequence();
	}
	/**
	 *  Perform a flatMap operation where the result will be a flattened stream of Strings
	 * from the text loaded from the supplied URLs 
	 * 
	 * <pre>
	 * {@code 
	 * List<String> result = anyM("input.file")
								.asSequence()
								.liftAndBindURL(getClass().getClassLoader()::getResource)
								.toList();
		
		assertThat(result,equalTo(Arrays.asList("hello","world")));
	 * 
	 * }
	 * </pre>
	 * 
	 * @param fn
	 * @return
	 */
	public final  SequenceM<String> liftAndBindURL(Function<? super T, URL> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).liftAndBind(fn).sequence();
	}
	/**
	  *  Perform a flatMap operation where the result will be a flattened stream of Strings
	 * from the text loaded from the supplied BufferedReaders
	 * 
	 * <pre>
	 * List<String> result = anyM("input.file")
								.asSequence()
								.map(getClass().getClassLoader()::getResourceAsStream)
								.map(InputStreamReader::new)
								.liftAndBindBufferedReader(BufferedReader::new)
								.toList();
		
		assertThat(result,equalTo(Arrays.asList("hello","world")));
	 * 
	 * </pre>
	 * 
	 * 
	 * @param fn
	 * @return
	 */
	public final SequenceM<String> liftAndBindBufferedReader(Function<? super T,BufferedReader> fn) {
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).liftAndBind(fn).sequence();
	}
	public final   SequenceM<T>  filter(Predicate<? super T> fn){
		return AsGenericMonad.<LazySeq<T>,T>asMonad(monad).filter(fn).sequence();
	}
	public void forEach(Consumer<? super T> action) {
		monad.forEach(action);
		
	}
}
