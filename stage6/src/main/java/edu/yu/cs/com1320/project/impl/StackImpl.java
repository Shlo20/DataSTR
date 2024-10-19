package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

import java.util.Deque;
import java.util.LinkedList;

public class StackImpl<T> implements Stack<T> {
    private final Deque<T> stack;

    public StackImpl() {
        this.stack = new LinkedList<>();
    }

    @Override
    public void push(T element) {
        stack.push(element);
    }

    @Override
    public T pop() {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.pop();
    }

    @Override
    public T peek() {
        return stack.peek();
    }

    @Override
    public int size() {
        return stack.size();
    }
}
