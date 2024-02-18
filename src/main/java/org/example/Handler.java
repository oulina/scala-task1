package org.example;

import java.time.Duration;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public interface Handler {
    default ApplicationStatusResponse performOperation(String id){
        AtomicInteger delayMethod = new AtomicInteger(15);
        Timer timer = new Timer( "timerForDelayMethod", true );
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                delayMethod.getAndDecrement();
                if (delayMethod.get() == 0) {
                    timer.cancel();
                }
            }
        }, 1000, 1000);

        AtomicBoolean existAnswer = new AtomicBoolean(false);
        AtomicInteger retriesCount = new AtomicInteger();
        AtomicReference<Response> result = new AtomicReference<>();
        AtomicReference<Date> lastDate = new AtomicReference<>(new Date());

        Client client = new Client() {
            @Override
            public Response getApplicationStatus1(String id) {
                return new Response.RetryAfter(Duration.ofSeconds(5));
                //return new Response.Success("success", "123");
                //return new Response.Failure(new Exception());
                //return new Response.RetryAfter(Duration.ofSeconds(2));
            }

            @Override
            public Response getApplicationStatus2(String id) {
                return new Response.RetryAfter(Duration.ofSeconds(2));
            }
        };

        Thread thread1 = new Thread(() -> {
            while (!existAnswer.get() && delayMethod.get() > 0) {
                Response answer1 = client.getApplicationStatus1(id);
                if (answer1 instanceof Response.Success) {
                    existAnswer.set(true);
                    result.set(answer1);
                }
                else if (answer1 instanceof Response.RetryAfter) {
                    retriesCount.getAndIncrement();
                    Duration sleep = ((Response.RetryAfter) answer1).delay();
                    if (sleep != null) {
                        try {
                            Thread.sleep(sleep.toSeconds() > delayMethod.get() ?
                                    delayMethod.get() * 1000L : sleep.toMillis());
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                else {
                    retriesCount.getAndIncrement();
                    lastDate.set(new Date());
                }
            }
        });
        Thread thread2 = new Thread(() -> {
            while (!existAnswer.get() && delayMethod.get() > 0) {
                Response answer2 = client.getApplicationStatus2(id);
                if (answer2 instanceof Response.Success) {
                    existAnswer.set(true);
                    result.set(answer2);
                }
                else if (answer2 instanceof Response.RetryAfter) {
                    retriesCount.getAndIncrement();
                    Duration sleep = ((Response.RetryAfter) answer2).delay();
                    if (sleep != null) {
                        try {
                            Thread.sleep(sleep.toSeconds() > delayMethod.get() ?
                                    delayMethod.get() * 1000L : sleep.toMillis());
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                else {
                    retriesCount.getAndIncrement();
                    lastDate.set(new Date());
                }
            }
        });
        try {
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();
        }
        catch (InterruptedException ignored) {
        }
        if (result.get() != null && result.get() instanceof Response.Success success) {
            return new ApplicationStatusResponse.Success(success.applicationId(), success.applicationStatus());
        }
        return new ApplicationStatusResponse.Failure(Duration.ofSeconds(lastDate.get().getTime()), retriesCount.get());
    }
}
