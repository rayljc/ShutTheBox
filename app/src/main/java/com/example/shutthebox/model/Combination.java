package com.example.shutthebox.model;

import java.util.Set;

public class Combination {
    public boolean judge(int tar, Set<Integer> cho){
        int sum =0;
        for (int ele : cho){
            sum += ele;
        }
        if(sum==tar){
            return true;
        }else{
            return false;
        }
    }

}
