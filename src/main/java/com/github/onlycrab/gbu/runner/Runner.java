package com.github.onlycrab.gbu.runner;

/**
 * Entry point.
 *
 * @author Roman Rynkovich
 */
@SuppressWarnings("WeakerAccess")
public class Runner {
    public static void main(String[] args){
        try {
            String msg = new Executor().execute(args);
            if (msg != null) {
                System.out.println(msg);
            }
        } catch (Exception e) {
            System.out.println(Executor.printError(e.getMessage()));
        }
    }
}
