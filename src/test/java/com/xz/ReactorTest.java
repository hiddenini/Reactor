package com.xz;

import com.xz.hello.reactor.BusinessException;
import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReactorTest {

    @Test
    public void test1() {
        StepVerifier.create(Flux.range(1, 6)    // 1
                .map(i -> i * i))   // 2
                .expectNext(1, 4, 9, 16, 25, 36)    //3
                .expectComplete()
                .verify();// 4
    }


    /**
     * verifyComplete()相当于expectComplete().verify()。
     * <p>
     * 对于每一个字符串s，将其拆分为包含一个字符的字符串流；
     * 对每个元素延迟100ms；
     * 对每个元素进行打印（注doOnNext方法是“偷窥式”的方法，不会消费数据流）；
     * 验证是否发出了8个元素
     */
    @Test
    public void test2() {
        StepVerifier.create(
                Flux.just("flux", "mono")
                        .flatMap(s -> Flux.fromArray(s.split("\\s*"))   // 1
                                .delayElements(Duration.ofMillis(100))) // 2
                        .doOnNext(System.out::print)) // 3
                .expectNextCount(8) // 4
                .verifyComplete();
    }

    /**
     * 生成1-6的数据流
     * <p>
     * 过滤保留其中的奇数
     * <p>
     * 进行平方
     * <p>
     * 验证平方的结果是否正确
     */
    @Test
    public void test3() {
        StepVerifier.create(Flux.range(1, 6)
                .filter(i -> i % 2 == 1)    // 1
                .map(i -> i * i))
                .expectNext(1, 9, 25)   // 2
                .verifyComplete();
    }

    /**
     * 将这句话拆分为一个一个的单词并以每200ms一个的速度发出
     * <p>
     * zip将source1 和source2组合成一个新的流
     */
    @Test
    public void test4() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);  // 2
        Flux.zip(
                getZipDescFlux(),
                Flux.interval(Duration.ofMillis(200)))  // 3
                .subscribe(t -> System.out.println(t.getT1()), null, countDownLatch::countDown);    // 4
        countDownLatch.await(10, TimeUnit.SECONDS);     // 5
    }

    private Flux<String> getZipDescFlux() {
        String desc = "Zip two sources together, that is to say wait for all the sources to emit one element and combine these elements once into a Tuple2.";
        return Flux.fromArray(desc.split("\\s+"));  // 1
    }

    /**
     * 用fromCallable声明一个基于Callable的Mono；
     * <p>
     * 使用subscribeOn将任务调度到Schedulers内置的弹性线程池执行，弹性线程池会为Callable的执行任务分配一个单独的线程。
     */
    @Test
    public void testSyncToAsync() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Mono.fromCallable(() -> getStringSync())    // 1
                .subscribeOn(Schedulers.elastic())  // 2
                .subscribe(System.out::println, null, countDownLatch::countDown);
        countDownLatch.await(10, TimeUnit.SECONDS);
    }


    private String getStringSync() {
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "Hello, Reactor!";
    }

    /**
     * 在响应式流中，错误（error）是终止信号。当有错误发生时，它会导致流序列停止，并且错误信号会沿着操作链条向下传递
     * <p>
     * 直至遇到subscribe中的错误处理方法。这样的错误还是应该在应用层面解决的。否则，你可能会将错误信息显示在用户界面
     * <p>
     * 或者通过某个REST endpoint发出。所以还是建议在subscribe时通过错误处理方法妥善解决错误
     */
    @Test
    public void testErrorHandling() {
        Flux.range(1, 6)
                .map(i -> 10 / (i - 3)) // 1
                .map(i -> i * i)
                .subscribe(System.out::println, System.err::println);
    }

    /**
     * 1. 捕获并返回一个静态的缺省值
     * <p>
     * 当发生异常时提供一个缺省值0
     */
    @Test
    public void testError1() {
        Flux.range(1, 6)
                .map(i -> 10 / (i - 3))
                .onErrorReturn(0)   // 1
                .map(i -> i * i)
                .subscribe(System.out::println, System.err::println);
    }


    /**
     * 捕获并执行一个异常处理方法或计算一个候补值来顶替
     * <p>
     * onErrorResume方法能够在收到错误信号的时候提供一个新的数据流
     * <p>
     * 更有业务含义的例子
     * <p>
     * Flux.just(endpoint1, endpoint2)
     * .flatMap(k -> callExternalService(k))   // 1
     * .onErrorResume(e -> getFromCache(k));   // 2
     * <p>
     * 调用外部服务；
     * 如果外部服务异常，则从缓存中取值代替。
     */
    @Test
    public void testError2() {
        Flux.range(1, 6)
                .map(i -> 10 / (i - 3))
                .onErrorResume(e -> Mono.just(new Random().nextInt(6))) // 提供新的数据流
                .map(i -> i * i)
                .subscribe(System.out::println, System.err::println);
    }

    /**
     * 捕获，并再包装为某一个业务相关的异常，然后再抛出业务异常
     */
    @Test
    public void testError3() {
        Flux.range(1, 6)
                .map(i -> 10 / (i - 3))   // 1
                .onErrorMap(original -> new BusinessException("除数不能为0", original))
                .subscribe();
    }

    /**
     * 捕获，记录错误日志，然后继续抛出
     */
    @Test
    public void testError4() {
        Flux.range(1, 6)
                .map(i -> 10 / (i - 3))   // 1
                .doOnError(e -> {
                    System.out.println("exception:" + e.getMessage());
                })
                .onErrorResume(e -> Mono.just(new Random().nextInt(6)))
                .subscribe(System.out::println);
    }

    /**
     * Flux.range是一个快的Publisher；
     * 在每次request的时候打印request个数；
     * 通过重写BaseSubscriber的方法来自定义Subscriber；
     * hookOnSubscribe定义在订阅的时候执行的操作；
     * 订阅时首先向上游请求1个元素；
     * hookOnNext定义每次在收到一个元素的时候的操作；
     * sleep 1秒钟来模拟慢的Subscriber；
     * 打印收到的元素；
     * 每次处理完1个元素后再请求1个
     */
    @Test
    public void testBackpressure() {
        Flux.range(1, 6)    // 1
                .doOnRequest(n -> System.out.println("Request " + n + " values..."))    // 2
                .subscribe(new BaseSubscriber<Integer>() {  // 3
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) { // 4
                        System.out.println("Subscribed and make a request...");
                        request(1); // 5
                    }

                    @Override
                    protected void hookOnNext(Integer value) {  // 6
                        try {
                            TimeUnit.SECONDS.sleep(1);  // 7
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Get value [" + value + "]");    // 8
                        request(1); // 9
                    }
                });
    }


}
