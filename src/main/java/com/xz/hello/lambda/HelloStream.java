package com.xz.hello.lambda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * stream是高級的迭代器,他不是一個數據結構,不是一個集合,不存放數據
 * <p>
 * 關注的是數據如何處理，類似流水綫
 */
public class HelloStream {

    /**
     * 外部迭代
     */
    public static void iteOutSide() {
        int nums[] = {1, 2, 3};

        int sum = 0;
        for (int num : nums) {
            sum += num;
        }
        System.out.println("sum:" + sum);
    }

    /**
     * 内部迭代
     */
    public static void iteInSide() {
        int nums[] = {1, 2, 3};

        int sum = IntStream.of(nums).sum();

        System.out.println("sum:" + sum);

    }

    /**
     * 惰性求值
     */
    public static void lazy() {
        int nums[] = {1, 2, 3};
        /**
         * map是中間操作,返回流的操作
         *
         * sum就是終止操作
         */
        int sum = IntStream.of(nums).map(i -> i * 2).sum();

        System.out.println("sum:" + sum);

        /**
         * 惰性求值就是終止操作沒有調用的情況下,中間操作不會執行
         */

        System.out.println("惰性求值就是終止操作沒有調用的情況下,中間操作不會執行");

        IntStream.of(nums).map(i -> i * 2).map(HelloStream::doubleNum);
    }

    public static int doubleNum(int i) {
        System.out.println("*2~~~");
        return i * 2;
    }


    /**
     * 創建流
     */
    public static void createStream() {
        //從集合創建
        List<String> list = new ArrayList<>();
        list.stream();
        list.parallelStream();

        //從數組創建
        Arrays.stream(new int[]{1, 2, 3});

        //創建數字流
        IntStream.of(1, 2, 3);
        IntStream.range(1, 10);

        //使用random創建一個無限流
        new Random().ints().limit(10);

        Random random = new Random();

        Stream.generate(() -> random.nextInt()).limit(20);
    }

    public static void main(String[] args) {
        iteOutSide();
        iteInSide();
        lazy();
    }


}
