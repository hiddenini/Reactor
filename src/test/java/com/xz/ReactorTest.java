package com.xz;

import com.xz.hello.reactor.BusinessException;
import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 详见   https://htmlpreview.github.io/?https://github.com/get-set/reactor-core/blob/master-zh/src/docs/index.html#about-doc
 */

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


    @Test
    public void test() {

    }

    /**
     * 可编程式地创建一个序列Generate
     */


    /**
     * 基于状态值的 generate 示例
     * 初始化状态值（state）为0。
     * 我们基于状态值 state 来生成下一个值（state 乘以 3）。
     * 我们也可以用状态值来决定什么时候终止序列。
     * 返回一个新的状态值 state，用于下一次调用。
     */

    @Test
    public void testGenerate() {
        Flux<String> flux = Flux.generate(
                () -> 0,
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3 * state);
                    if (state == 10) sink.complete();
                    return state + 1;
                });
        flux.subscribe(System.out::println);
    }

    /**
     * 可变类型的状态变量
     */
    @Test
    public void testGenerate1() {
        Flux<String> flux = Flux.generate(
                AtomicLong::new,
                (state, sink) -> {
                    long i = state.getAndIncrement();
                    sink.next("3 x " + i + " = " + 3 * i);
                    if (i == 9) sink.complete();
                    return state;
                });
        flux.subscribe(System.out::println);
    }

    @Test
    public void testGenerate2() {
        Flux<String> flux = Flux.generate(
                AtomicLong::new,
                (state, sink) -> {
                    long i = state.getAndIncrement();
                    sink.next("3 x " + i + " = " + 3 * i);
                    if (i == 10) sink.complete();
                    return state;
                }, (state) -> System.out.println("state: " + state));
        flux.subscribe(System.out::println);
    }

    /**
     * 可编程式地创建一个序列Create
     * <p>
     * create 有个好处就是可以将现有的 API 转为响应式，比如监听器的异步方法。
     */

    @Test
    public void testCreate() {
        Flux.create(sink -> {
            for (int i = 0; i < 10; i++) {
                sink.next(i);
            }
            sink.complete();
        }).subscribe(System.out::println);
    }


    interface MyEventListener<T> {
        void onDataChunk(List<T> chunk);

        void processComplete();
    }


    /**
     * 官网的Create和push暂时不知道myEventProcessor是什么
     */
    @Test
    public void testCreate1() {
/*        Flux<String> bridge = Flux.create(sink -> {
            myEventProcessor.register(
                    new MyEventListener<String>() {

                        public void onDataChunk(List<String> chunk) {
                            for (String s : chunk) {
                                sink.next(s);
                            }
                        }

                        public void processComplete() {
                            sink.complete();
                        }
                    });
        });*/
    }

    /**
     * Handle
     * <p>
     * handle 方法有些不同，它在 Mono 和 Flux 中都有。然而，它是一个实例方法 （instance method），意思就是它要链接在一个现有的源后使用（与其他操作符一样
     */
    @Test
    public void testHandle() {
        Flux<String> alphabet = Flux.just(-1, 30, 13, 9, 20)
                .handle((i, sink) -> {
                    String letter = alphabet(i);
                    if (letter != null)
                        sink.next(letter);
                });

        alphabet.subscribe(System.out::println);
    }

    public String alphabet(int letterNumber) {
        if (letterNumber < 1 || letterNumber > 26) {
            return null;
        }
        int letterIndexAscii = 'A' + letterNumber - 1;
        return "" + (char) letterIndexAscii;
    }


    /**
     * 调度器（Schedulers）
     * <p>
     * Scheduler 是一个拥有广泛实现类的抽象接口。 Schedulers 类提供的静态方法用于达成如下的执行环境
     * <p>
     * 当前线程（Schedulers.immediate()）
     * <p>
     * 可重用的单线程（Schedulers.single()) 注意，这个方法对所有调用者都提供同一个线程来使用， 直到该调度器（Scheduler）被废弃。如果你想使用专一的线程，就对每一个调用使用 Schedulers.newSingle()
     * <p>
     * 弹性线程池（Schedulers.elastic()   它根据需要创建一个线程池，重用空闲线程。线程池如果空闲时间过长 （默认为 60s）就会被废弃。对于 I/O 阻塞的场景比较适用。 Schedulers.elastic() 能够方便地给一个阻塞 的任务分配它自己的线程，从而不会妨碍其他任务和资源
     * <p>
     * 固定大小线程池（Schedulers.parallel())   所创建线程池的大小与 CPU 个数等同
     */

    @Test
    public void testSchedulers1() throws InterruptedException, IOException {
        System.out.println("main thread:" + Thread.currentThread().getId());
        CountDownLatch countDownLatch = new CountDownLatch(1);  // 2
        Flux.interval(Duration.ofMillis(300), Schedulers.newSingle("test")).subscribe((n -> {
            System.out.println(n);
            System.out.println(Thread.currentThread().getId() + "----" + Thread.currentThread().getName());
        }), null, countDownLatch::countDown);

        countDownLatch.await(10, TimeUnit.SECONDS);     // 5

/*        Flux.interval(Duration.ofMillis(300), Schedulers.newSingle("test")).limitRequest(10).subscribe(System.out::println);;
        System.in.read();*/
    }

    /**
     * Reactor 提供了两种在响应式链中调整调度器 Scheduler 的方法：publishOn 和 subscribeOn。
     *
     * 它们都接受一个 Scheduler 作为参数，从而可以改变调度器。但是 publishOn 在链中出现的位置 是有讲究的，而 subscribeOn 则无所谓。
     *
     * 在 Reactor 中，当你在操作链上添加操作符的时候，你可以根据需要在 Flux 和 Mono 的实现中包装其他的 Flux 和 Mono。一旦你订阅（subscribe）了它
     *
     * 一个 Subscriber 的链 就被创建了，一直向上到第一个 publisher 。这些对开发者是不可见的，开发者所能看到的是最外一层的 Flux （或 Mono）和 Subscription
     *
     * 但是具体的任务是在中间这些跟操作符相关的 subscriber 上处理的
     *
     * publishOn 的用法和处于订阅链（subscriber chain）中的其他操作符一样。它将上游 信号传给下游，同时执行指定的调度器 Scheduler 的某个工作线程上的回调。
     *
     * 它会 改变后续的操作符的执行所在线程 （直到下一个 publishOn 出现在这个链上）
     *
     * subscribeOn 用于订阅（subscription）过程，作用于那个向上的订阅链（发布者在被订阅 时才激活，订阅的传递方向是向上游的）。
     *
     * 所以，无论你把 subscribeOn 至于操作链的什么位置， 它都会影响到源头的线程执行环境（context）。 但是，它不会影响到后续的 publishOn，后者仍能够切换其后操作符的线程执行环境
     *
     * 只有操作链中最早的 subscribeOn 调用才算数。
     *
     * subscribe() 之前什么都不会发生
     *
     * 在 Reactor 中，当你创建了一条 Publisher 处理链，数据还不会开始生成。事实上，你是创建了 一种抽象的对于异步处理流程的描述（从而方便重用和组装）。
     *
     * 当真正“订阅（subscrib）”的时候，你需要将 Publisher 关联到一个 Subscriber 上，然后 才会触发整个链的流动。这时候，Subscriber 会向上游发送一个 request 信号，一直到达源头 的 Publisher
     *
     */


    /**
     * 调试
     */


    /**
     * Hooks.onOperatorDebug();
     * <p>
     * 用这种形式的检测方式构造 stack trace 是成本较高的。也因此这种调试模式作为最终大招， 只应该在可控的方式下激活。
     */
    @Test
    public void testBug() {
        Hooks.onOperatorDebug();

        Flux.range(1, 6)
                .map(i -> 10 / (i - 3))
                .map(i -> i * i)
                .subscribe();
    }


    /**
     * 用 checkpoint() 方式替代
     * <p>
     * 如果你能确定是在你的代码中组装的响应式链存在问题，而且程序的可服务性又是很重要的， 那么你可以 使用 checkpoint() 操作符。
     * <p>
     * 它有两种调试技术可用
     * <p>
     * 1--你可以把这个操作符加到链中。这时 checkpoint 操作符就像是一个 hook，但只对它所在的链起作用。
     * <p>
     * 2--还有一个 checkpoint(String) 的方法变体，你可以传入一个独特的字符串以方便在 assembly traceback 中识别信息。
     * <p>
     * 这样会省略 stack trace，你可以依赖这个字符串（以下改称“定位描述符”）来定位到组装点。checkpoint(String) 比 checkpoint 有更低的执行成本。
     * <p>
     * 最后的但同样重要的是，如果你既想通过 checkpoint 添加定位描述符，同时又依赖于 stack trace 来定位组装点，你可以使用 checkpoint("description", true)
     * <p>
     * 来实现这一点。这时回溯信息又出来了， 同时附加了定位描述符，如下例所示
     */
    @Test
    public void testBug1() {
        Flux.range(1, 6)
                .map(i -> 10 / (i - 3))
                .map(i -> i * i)
                .checkpoint()
                .subscribe();
    }

    @Test
    public void testBug2() {
        Flux.range(1, 6)
                .map(i -> 10 / (i - 3))
                .map(i -> i * i)
                .checkpoint("bug~~~~~~")
                .subscribe();
    }

    @Test
    public void testBug3() {
        Flux.range(1, 6)
                .map(i -> 10 / (i - 3))
                .map(i -> i * i)
                .checkpoint("bug~~~~~~", true)
                .subscribe();
    }

    /**
     * log
     */
    @Test
    public void testLog() {
        Flux.range(1, 10)
                .log()
                .take(3).subscribe(System.out::println);
    }
}

