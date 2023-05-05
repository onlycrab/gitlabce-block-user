package com.github.onlycrab.gbu.worker;

import javax.naming.NamingEnumeration;
import java.util.*;

@SuppressWarnings("WeakerAccess")
class TestNamingEnumeration<T> implements NamingEnumeration<T> {
    private final LinkedList<T> list;

    public TestNamingEnumeration(){
        list = new LinkedList<>();
    }

    public void add(T sr){
        if (sr != null){
            list.addLast(sr);
        }
    }

    @Override
    public T next() {
        return list.poll();
    }

    @Override
    public boolean hasMore() {
        return list.size() != 0;
    }

    @Override
    public void close() {
        list.clear();
    }

    @Override
    public boolean hasMoreElements() {
        return list.size() != 0;
    }

    @Override
    public T nextElement() {
        return list.poll();
    }
}
