package com.xz.hello.lambda;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.*;

public class HelloLambda {

    public void threadLambda() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("ok");
            }
        }).start();


        //jdk1.8 lambda  返回了實現指定接口的實例
        new Thread(() ->
                System.out.println("ok")
        ).start();

    }

    /**
     * 帶參數的lambda寫法
     */
    public void paramLambda() {
        NumInterface numInterface1 = (num) -> num * 2;

        /**
         * 最常見的寫法,參數個數為1,括號可以去掉
         */
        NumInterface numInterface2 = num -> num * 2;

        NumInterface numInterface3 = (int num) -> num * 2;

        NumInterface numInterface4 = (int num) -> {
            System.out.println("num:" + num);
            return num * 2;
        };

    }

    interface NumInterface {
        int doubleNum(int num);
    }

    /**
     * jdk8接口新特性
     * <p>
     * 1--函數接口 只有一個抽象方法的接口
     * <p>
     * 2--可以有默認方法
     */
    public static void character() {
        SomeInterface someInterface = i -> i * 2;
        System.out.println(someInterface.add(1, 2));
        System.out.println(someInterface.doubleNum(3));
    }

    @FunctionalInterface
    interface SomeInterface {
        int doubleNum(int num);

        /**
         * List接口在1.2版本直到1.8幾乎沒變過,直到default方法出現
         * <p>
         * 因爲在接口中新添加方法那麽所有實現了這個接口的類都需要實現這個方法
         */
        default int add(int x, int y) {
            return x + y;
        }
    }

    /**
     * jdk自帶的函數接口
     */
    interface IMoneyFormat {
        String format(int i);
    }


    static class MyMoney {

        private final int money;


        public MyMoney(int money) {
            this.money = money;
        }

        public void printMoney(IMoneyFormat moneyFormat) {
            System.out.println("我的存款:" + moneyFormat.format(this.money));
        }

        /**
         * 使用Function代替IMoneyFormat接口
         */
        public void printMoney1(Function<Integer, String> moneyFormat) {
            System.out.println("我的存款:" + moneyFormat.apply(this.money));
        }

    }

    /**
     * Predicate 接受一個數據,返回boolean值
     */
    public static void pre() {
        Predicate<Integer> predicate = i -> i > 0;
        System.out.println(predicate.test(9));

        /**
         * jdk有默認的基本數據類型的函數接口
         */
        IntPredicate predicate1 = i -> i > 10;
    }

    /**
     * consumer  接受一個數據并消費他,沒有返回值
     */
    public static void con() {
        Consumer<String> consumer = s -> System.out.println(s);
        consumer.accept("我是一條隨意的數據");
    }

    /**
     * Supplier 沒有輸入,提供一個數據
     */
    public static void sup() {
        Supplier<String> stringSupplier = () -> "我是一個被提供的字符串";
        System.out.println(stringSupplier.get());
    }

    /**
     * Function<Integer, String> s輸入Integer返回String
     */
    public static void fun() {
        Function<Integer, String> function = i -> String.valueOf("employee" + i);
        System.out.println(function.apply(5));
    }

    /**
     * 方法引用
     */
    public static void method() {
        //Consumer<String> consumer = s -> System.out.println(s);
        //儅函數的執行體只有一個函數調用，并且參數和lambda的參數一致，那麽可以使用方法引用
        Consumer<String> consumer = System.out::println;
        consumer.accept("我是一個被消費的字符串");
    }

    /**
     * 靜態方法的引用
     */
    public static void staticMethodRef() {
        Consumer<Dog> consumer = Dog::bark;
        Dog dog = new Dog();
        consumer.accept(dog);
    }

    /**
     * 非靜態方法的引用 使用對象實例引用
     */
    public static void nonStaticMethodRef() {
        Dog dog = new Dog();
        Function<Integer, Integer> function = dog::eat;
        System.out.println("還剩下:" + function.apply(1) + "斤");

        //函數接口的輸入和輸出一致時可以修改為一元函數接口
        UnaryOperator<Integer> unaryOperator = dog::eat;
        System.out.println("還剩下:" + unaryOperator.apply(2) + "斤");

        //基本數據類型有默認的函數接口
        IntUnaryOperator intUnaryOperator = dog::eat;
        System.out.println("還剩下:" + intUnaryOperator.applyAsInt(3) + "斤");

        BiFunction<Dog, Integer, Integer> biFunction = Dog::eat;
        System.out.println("還剩下:" + biFunction.apply(dog, 2) + "斤");

    }

    /**
     * 構造函數的方法引用
     */
    public static void construct() {
        Supplier<Dog> supplier = Dog::new;
        System.out.println("創建了對象" + supplier.get());

        Function<String, Dog> function = Dog::new;
        System.out.println("創建了對象" + function.apply("jack"));
    }

    /**
     * Type
     */
    public void type() {
        //變量類型定義
        IMath iMath = (x, y) -> x + y;
        //數組定義
        IMath[] iMaths = {(x, y) -> x + y};
        //強轉
        Object object = (IMath) (x, y) -> x + y;
        //返回類型
        createIMath();

    }

    public static IMath createIMath() {
        return (x, y) -> x + y;
    }

    public void testType(IMath iMath) {

    }

    public void testType(IMath1 iMath) {

    }


    interface IMath {
        int add(int x, int y);
    }

    interface IMath1 {
        int add(int x, int y);
    }

    /**
     * 變量引用
     */
    public static void var() {
        /**
         * java是傳值不是傳引用,所以lambda使用的變量一定要是final的
         *
         * 因爲list 和lambda的list都是指向的是new ArrayList<>()
         *
         * 如果在lambda之外修改了list,那麽在lambda中再去修改list返回的結果可能是不對的 或者說二義性
         */
        List<String> list = new ArrayList<>();
        Consumer<String> consumer = s -> System.out.println(s + list);
        consumer.accept("world");
    }

    /**
     * 級聯表達式和柯里化
     * <p>
     * 柯里化:把多個參數的函數轉化為只有一個參數的函數
     * <p>
     * 柯里化的目的：使函數標準化
     */
    public static void cascade() {
        Function<Integer, Function<Integer, Integer>> function = x -> y -> x + y;

        System.out.println(function.apply(2).apply(3));

        Function<Integer, Function<Integer, Function<Integer, Integer>>> function1 = x -> y -> z -> x + y + z;

        System.out.println(function1.apply(2).apply(3).apply(5));

        int nums[] = {2, 3, 4};
        Function f = function1;
        for (int i = 0; i < nums.length; i++) {
            if (f instanceof Function) {
                Object apply = f.apply(nums[i]);

                if (apply instanceof Function) {
                    f = (Function) apply;
                } else {
                    System.out.println("調用結束,結果爲:" + apply);
                }
            }
        }

    }


    public static void main(String[] args) {
        //character();

        MyMoney myMoney = new MyMoney(99999999);
        myMoney.printMoney(i -> new DecimalFormat("#,###").format(i));

        Function<Integer, String> moneyFormat = i -> new DecimalFormat("#,###").format(i);
        myMoney.printMoney1(moneyFormat.andThen(s -> "rmb" + s));

        pre();

        con();

        sup();

        fun();

        method();

        staticMethodRef();

        nonStaticMethodRef();

        construct();

        HelloLambda helloLambda = new HelloLambda();

        /**
         * 黨有二義性的時候，使用强轉解決
         */
        helloLambda.testType((IMath) (x, y) -> x + y);

        cascade();

    }

}
