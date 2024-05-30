package com.xuecheng.checkcode;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class CTest {

    AImpl a = new AImpl();

    @Test
    public void test1() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        ListIterator<Integer> listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            Integer i = listIterator.next();
            if (i == 5) listIterator.set(10);
            if (i == 7) {
                listIterator.remove();
                break;
            }
            System.out.println(i);
        }
        System.out.println(list);


        try {
            list.forEach(integer -> {
                if (integer == 5) return;
                if (integer == 7) {
                    throw new RuntimeException();
                }

                System.out.println(integer);
            });
        } catch (Exception e) {
            System.out.println("循环结束了。。。");
        }


    }

    class B{
        public void f3(){
            a.f1();
        };
    }

    @Test
    public void test2() {
        new A() {
            @Override
            public void f1() {
                System.out.println("--");
                a.f1();
            }
            @Override
            public void f2() {

            }
        }.f1();

        B b = new B();
        b.f3();
    }
}
