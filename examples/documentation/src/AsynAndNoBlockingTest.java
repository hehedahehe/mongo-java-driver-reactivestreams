import org.junit.Test;

import java.util.concurrent.*;
import java.util.function.Supplier;


/**
 * https://www.baeldung.com/java-completablefuture
 * https://juejin.im/post/5adbf8226fb9a07aac240a67
 * CompletableFuture
 * 提供了任务编排的能力
 * The Future interface was added in Java 5 to serve as a result of an asynchronous computation,
 * but it did not have any methods to combine these computations or handle possible errors.
 *
 * In Java 8, the CompletableFuture class was introduced. Along with the Future interface,
 * it also implemented the CompletionStage interface. This interface defines the contract for an asynchronous
 * computation step that can be combined with other steps.
 */
public class AsynAndNoBlockingTest {

    String f = "res %s res %s cost %s";
    String f1 = "%s time %s";

    @Test
    public void testFutureAndComplatabelFuture() {
        ServiceRemote serviceRemoteA = new ServiceRemote("a");
        ServiceRemote serviceRemoteB = new ServiceRemote("b");

        long time1 = System.currentTimeMillis();

        String res = "";

        String msgDepend = serviceRemoteA.get();
        String msg = serviceRemoteB.get() + (msgDepend);

        long time2 = System.currentTimeMillis();

        System.out.println(String.format(f, msg, (time2 - time1) / 1000));
    }

    /**
     * 有没有依赖需要在业务层面进行控制
     * 两个Task没有依赖
     */
    @Test
    public void testFuture() {
        ServiceRemote serviceRemoteA = new ServiceRemote("a");
        ServiceRemote serviceRemoteB = new ServiceRemote("b");

        ExecutorService executor = Executors.newCachedThreadPool();

        long time1 = System.currentTimeMillis();

        Task taskA = new Task();
        taskA.setServiceRemote(serviceRemoteA);
        Future<String> resultA = executor.submit(taskA);//t1=new Thread().start()
        System.out.println(String.format(f1, 1, System.currentTimeMillis()));

        Task taskB = new Task();
        taskB.setServiceRemote(serviceRemoteB);
        Future<String> resultB = executor.submit(taskB);//t2=new Thread().start()
        System.out.println(String.format(f1, 2, System.currentTimeMillis()));

        String msgA = "";
        String msgB = "";
        try {
            msgA = resultA.get();//wait for t1 to complete taskA
            System.out.println(String.format(f1, 3, System.currentTimeMillis()) + "===" + msgA);

            msgB = resultB.get();//wait for t2 to complete taskB
            System.out.println(String.format(f1, 4, System.currentTimeMillis()) + "===" + msgB);

        } catch (Exception e1) {
            e1.printStackTrace();
        }

        executor.shutdown();
        System.out.println(String.format(f1, 5, System.currentTimeMillis()));

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        long time2 = System.currentTimeMillis();

        System.out.println(String.format(f, msgA, msgB, (time2 - time1) / 1000));

    }

    /**
     * 有没有依赖需要在业务层面进行控制
     * 两个Task有依赖
     */
    @Test
    public void testFutureDependChain() {
        ServiceRemote serviceRemoteA = new ServiceRemote("a");
        ExecutorService executor = Executors.newCachedThreadPool();

        long time1 = System.currentTimeMillis();

        Task taskA = new Task();
        taskA.setServiceRemote(serviceRemoteA);
        Future<String> resultA = executor.submit(taskA);//new Thread().start()
        System.out.println(String.format(f1, 1, System.currentTimeMillis()));

        String msgA = "";

        try {
            msgA = resultA.get();
            System.out.println(String.format(f1, 3, System.currentTimeMillis()) + "===" + msgA);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        ServiceRemote serviceRemoteB = new ServiceRemote("b" + "******" + msgA);
        Task taskB = new Task();
        taskB.setServiceRemote(serviceRemoteB);
        Future<String> resultB = executor.submit(taskB);//new Thread().start()
        System.out.println(String.format(f1, 2, System.currentTimeMillis()));

        String msgB = "";
        try {
            msgB = resultB.get();
            System.out.println(String.format(f1, 4, System.currentTimeMillis()) + "===" + msgB);

        } catch (Exception e1) {
            e1.printStackTrace();
        }

        executor.shutdown();
        System.out.println(String.format(f1, 5, System.currentTimeMillis()));

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        long time2 = System.currentTimeMillis();

        System.out.println(String.format(f, msgA, msgB, (time2 - time1) / 1000));
    }

    /**
     * 前后task有依赖的，都是同步操作。
     *
     * @throws Exception
     */
    @Test
    public void testCompletableFutureDependChainAsyn() throws Exception {
        long time1 = System.currentTimeMillis();
        System.out.println("0" + Thread.currentThread());
        CompletableFuture<String> composeStringFuture =
                CompletableFuture
                        .supplyAsync(new Supplier<String>() {
                            @Override
                            public String get() {
                                System.out.println("1" + Thread.currentThread());
                                return new ServiceRemote("a").get();
                            }
                        })
                        .thenApply(msgFrom1 -> {
                            System.out.println("2" + Thread.currentThread());
                            return msgFrom1 + "***" + new ServiceRemote("b").get();
                        });
        String res = composeStringFuture.get();
        System.out.println(res);
        long time2 = System.currentTimeMillis();
        System.out.println(time2 - time1);
    }

    /**
     * 使用步骤
     * 1. 从业务角度确定任务的并发模型。
     * <p>
     * 前后task有依赖的，都是同步操作。
     * 但是感觉没有必要交给线程池
     *
     * @throws Exception
     */
    @Test
    public void testCompletableFutureDependChainyn() throws Exception {
        long time1 = System.currentTimeMillis();
        System.out.println("0" + Thread.currentThread());
        CompletableFuture<String> composeStringFuture =
                CompletableFuture.supplyAsync(new Supplier<String>() {
                    @Override
                    public String get() {
                        System.out.println("1" + Thread.currentThread());
                        return new ServiceRemote("a").get();
                    }
                })
                        .thenApply(msgFrom1 -> {
                            System.out.println("2" + Thread.currentThread());
                            return msgFrom1 + "***" + new ServiceRemote("b").get();
                        });
        System.out.println("00000000" + Thread.currentThread());
        String res = composeStringFuture.get();
        System.out.println(res);
        long time2 = System.currentTimeMillis();
        System.out.println(time2 - time1);
    }

    /**
     * 使用步骤
     * 1. 从业务角度确定任务的并发模型。
     * <p>
     * 前后task有依赖的，都是同步操作。
     * 但是感觉没有必要交给线程池
     *
     * @throws Exception
     */
    @Test
    public void testCompletableFutureDependChainyn2() throws Exception {
        long time1 = System.currentTimeMillis();
        System.out.println("0" + Thread.currentThread());
        CompletableFuture<String> composeStringFuture =
                CompletableFuture
                        .supplyAsync(new Supplier<String>() {
                            @Override
                            public String get() {
                                System.out.println("1" + Thread.currentThread());
                                return new ServiceRemote("a").get();
                            }
                        })
                        .thenApplyAsync(msgFrom1 -> {
                            System.out.println("2" + Thread.currentThread());
                            return msgFrom1 + "***" + new ServiceRemote("b").get();
                        });
        System.out.println("00000000" + Thread.currentThread());
        String res = composeStringFuture.get();
        System.out.println(res);
        long time2 = System.currentTimeMillis();
        System.out.println(time2 - time1);
    }


    class ServiceRemote {
        private String message;

        public ServiceRemote(String msg) {
            this.message = msg;
        }

        public String get() {
            try {
                Thread.sleep(1000L);
            } catch (Exception e) {

            }
            return "message from " + message;
        }
    }


    public class Task implements Callable<String> {
        private ServiceRemote serviceRemote;

        public void Task(ServiceRemote serviceRemote) {
            this.serviceRemote = serviceRemote;
        }

        public ServiceRemote getServiceRemote() {
            return serviceRemote;
        }

        public void setServiceRemote(ServiceRemote serviceRemote) {
            this.serviceRemote = serviceRemote;
        }

        @Override
        public String call() throws Exception {
            return serviceRemote.get();
        }
    }
}
