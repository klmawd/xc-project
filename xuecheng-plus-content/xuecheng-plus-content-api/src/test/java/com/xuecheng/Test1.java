package com.xuecheng;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Test1 {

    @Test
    public void test1() {
        String[] a = {"a", "b"};
        List<String> list = new ArrayList<>();

        //List<String> list1 = Arrays.asList(a);

        Collections.addAll(list, a);


        list.add("c");
        //list1.add("c");
        System.out.println(list);
       // System.out.println(list1);
    }


}
