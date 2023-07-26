package schedule;

import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;

import java.time.DateTimeException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BasicUse {

    public static class TestTask extends TimerTask {
        private String jobName;
        private long preTime;

        public  TestTask(String jobName){
            this.jobName = jobName;
        }
        @Override
        public void run() {

            System.out.println("job:"+jobName +"run at" + new Date());
        }
    }

    public static class QuartzTest implements Job{

        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
            System.out.println("任务 {"+ jobExecutionContext.getJobDetail().getKey().getName() +  "," +
                    jobExecutionContext.getJobDetail().getKey().getGroup() +
                    "}于 + "+ new Date() + " 执行，任务参数："+ jobExecutionContext.getJobDetail().getJobDataMap());
        }
    }

    public static void main(String[] args) throws SchedulerException {
//        Timer timer = new Timer();
//        timer.schedule(new TestTask("测试任务1"), 10000, 10000);
//
//        Calendar currentDate = Calendar.getInstance();
//
//        System.out.println(currentDate.get(Calendar.DAY_OF_WEEK));

        //启动调度器
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        //创建调度任务
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("testParam1", "参数值");
        jobDataMap.put("testParam2", "参数值");
        jobDataMap.put("testParam3", "参数值");

        JobDetail jobDetail = JobBuilder.newJob(QuartzTest.class)
                .withIdentity("测试任务", "测试组")
                .setJobData(jobDataMap)
                .build();
        //创建触发器
        Trigger jobTrigger = TriggerBuilder.newTrigger()
                .withIdentity("测试任务", "测试组")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.
                        simpleSchedule().withIntervalInSeconds(30).withRepeatCount(10))
                .build();
        //绑定触发器和任务，开启调度
        scheduler.scheduleJob(jobDetail, jobTrigger);
        scheduler.start();

        CronScheduleBuilder.cronSchedule(" 0 0 0/3  * * ?");
    }
}
