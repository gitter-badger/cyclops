package com.aol.cyclops.functionaljava;

import com.aol.simple.react.stream.traits.LazyFutureStream;
import com.google.common.collect.FluentIterable;
import com.nurkiewicz.lazyseq.LazySeq;

import org.jooq.lambda.Seq;

import java.util.stream.Stream;

/**
 * Created by johnmcclean on 4/8/15.
 */
public class ToStream {

    public static <T> FluentIterable<T> toFluentIterable(fj.data.Stream<T> s){
        return FluentIterable.from(s);
    }
    public static <T> Stream<T> toStream(fj.data.Stream<T> s){
        return Seq.seq(s.iterator());
    }
    public static <T> Seq<T> toJooqLambda(fj.data.Stream<T> s){
        return Seq.seq(s.iterator());
    }
    public static <T> LazyFutureStream<T> toFutureStream(fj.data.Stream<T> s){
        return LazyFutureStream.of(s.iterator());
    }

    public static <T> LazySeq<T> toLazySeq(fj.data.Stream<T> s){
        return LazySeq.of(s.iterator());
    }



}
