package com.xz.hello.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class BasicConcept {
    public static void main(String[] args) {
        /**
         * Reactor中的发布者（Publisher）由Flux和Mono两个类定义，它们都提供了丰富的操作符（operator）。
         *
         * 一个Flux对象代表一个包含0..N个元素的响应式序列，而一个Mono对象代表一个包含零/一个（0..1）元素的结果。
         *
         * 既然是“数据流”的发布者，Flux和Mono都可以发出三种“数据信号”：元素值、错误信号、完成信号，错误信号和完成信号都是终止信号
         *
         * 完成信号用于告知下游订阅者该数据流正常结束，错误信号终止数据流的同时将错误传递给下游订阅者。
         */
/*
        testJust();
        testList();
        testMon();*/


        /**
         * Flux和Mono提供了多种创建数据流的方法，just就是一种比较直接的声明数据流的方式，其参数就是数据元素。
         */
        Flux.just(1, 2, 3, 4, 5, 6);
        Mono.just(1);

        /**
         * 还可以通过如下方式声明（分别基于数组、集合和Stream生成）
         */

        Integer[] array = new Integer[]{1, 2, 3, 4, 5, 6};
        Flux.fromArray(array);
        List<Integer> list = Arrays.asList(array);
        Flux.fromIterable(list);
        Stream<Integer> stream = list.stream();
        Flux.fromStream(stream);


        /**
         * 订阅前什么都不会发生
         *
         * subscribe方法中的lambda表达式作用在了每一个数据元素上。此外，Flux和Mono还提供了多个subscribe方法的变体
         *
         * // 订阅并触发数据流
         * subscribe();
         *
         * // 订阅并指定对正常数据元素如何处理
         * subscribe(Consumer<? super T> consumer);
         *
         * // 订阅并定义对正常数据元素和错误信号的处理
         * subscribe(Consumer<? super T> consumer,
         *           Consumer<? super Throwable> errorConsumer);
         *
         * // 订阅并定义对正常数据元素、错误信号和完成信号的处理
         * subscribe(Consumer<? super T> consumer,
         *           Consumer<? super Throwable> errorConsumer,
         *           Runnable completeConsumer);
         *
         * // 订阅并定义对正常数据元素、错误信号和完成信号的处理，以及订阅发生时的处理逻辑
         * subscribe(Consumer<? super T> consumer,
         *           Consumer<? super Throwable> errorConsumer,
         *           Runnable completeConsumer,
         *           Consumer<? super Subscription> subscriptionConsumer);
         *
         *
         *这里需要注意的一点是，Flux.just(1, 2, 3, 4, 5, 6)仅仅声明了这个数据流，此时数据元素并未发出，只有subscribe()方法调用的时候才会触发数据流。所以，订阅前什么都不会发生
         *
         */
        Flux.just(1, 2, 3, 4, 5, 6).subscribe(System.out::print);
        System.out.println();
        Mono.just(1).subscribe(System.out::println);


        Flux.just(1, 2, 3, 4, 5, 6).subscribe(
                System.out::println,
                System.err::println,
                () -> System.out.println("Completed!"));


        Mono.error(new Exception("some error")).subscribe(
                System.out::println,
                System.err::println,
                () -> System.out.println("Completed!")
        );

        testViaStepVerifier();
    }

    public static void testViaStepVerifier() {
        StepVerifier.create(generateFluxFrom1To6())
                .expectNext(1, 2, 3, 4, 5, 6)
                .expectComplete()
                .verify();
        StepVerifier.create(generateMonoWithError())
                .expectErrorMessage("some error")
                .verify();
    }


    private static Flux<Integer> generateFluxFrom1To6() {
        return Flux.just(1, 2, 3, 4, 5, 6);
    }

    private static Mono<Integer> generateMonoWithError() {
        return Mono.error(new Exception("some error"));
    }


    public static void testJust() {
        Flux.just("hello", "world")
                .subscribe(System.out::println);
    }

    public static void testList() {
        List<String> words = Arrays.asList(
                "hello",
                "reactive",
                "world"
        );

        Flux.fromIterable(words)
                .subscribe(System.out::println);
    }

    public static void testRange() {
        Flux.range(1, 10)
                .subscribe(System.out::println);
    }

    public static void testInterval() {
        Flux.interval(Duration.ofSeconds(1))
                .subscribe(System.out::println);
    }

    public static void testMon() {
        displayCurrTime(1);
        displayCurrThreadId(1);
        //创建一个数据源
        Mono.just(10)
                //延迟5秒再发射数据
                .delayElement(Duration.ofSeconds(5))
                //在数据上执行一个转换
                .map(n -> {
                    displayCurrTime(2);
                    displayCurrThreadId(2);
                    displayValue(n);
                    delaySeconds(2);
                    return n + 1;
                })
                //在数据上执行一个过滤
                .filter(n -> {
                    displayCurrTime(3);
                    displayCurrThreadId(3);
                    displayValue(n);
                    delaySeconds(3);
                    return n % 2 == 0;
                })
                //如果数据没了就用默认值
                .defaultIfEmpty(9)
                //订阅一个消费者把数据消费了
                .subscribe(n -> {
                    displayCurrTime(4);
                    displayCurrThreadId(4);
                    displayValue(n);
                    delaySeconds(2);
                    System.out.println(n + " consumed, worker Thread over, exit.");
                });
        displayCurrTime(5);
        displayCurrThreadId(5);
        pause();
    }

    //显示当前时间
    static void displayCurrTime(int point) {
        System.out.println(point + " : " + LocalTime.now());
    }

    //显示当前线程Id
    static void displayCurrThreadId(int point) {
        System.out.println(point + " ThreadId: " + Thread.currentThread().getId());
    }

    //显示当前的数值
    static void displayValue(int n) {
        System.out.println("input : " + n);
    }

    //延迟若干秒
    static void delaySeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //主线程暂停
    static void pause() {
        try {
            System.out.println("main Thread over, paused.");
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
