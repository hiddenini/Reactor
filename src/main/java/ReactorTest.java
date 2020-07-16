import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ReactorTest {
    public static void main(String[] args) {
/*        testJust();

        testList();*/
        testMon();
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
