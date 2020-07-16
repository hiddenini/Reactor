package com.xz.hello.lambda;

public class Dog {
    private String name = "tom";

    private int food = 10;

    public Dog() {

    }

    /**
     * 帶參數的構造函數
     */
    public Dog(String name) {
        this.name = name;
    }

    public static void bark(Dog dog) {
        System.out.println(dog + "叫了");
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * jdk默認會把當前實例this傳入到非靜態方法中位置是第一個參數
     * <p>
     * 所以這裏可以手動添加一股額this,也可以不加
     */
    public int eat(Dog this, int num) {
        System.out.println("吃了:" + num + "斤狗糧");
        this.food -= num;
        return this.food;
    }

}
