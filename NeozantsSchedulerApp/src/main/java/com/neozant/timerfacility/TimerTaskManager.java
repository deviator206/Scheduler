package com.neozant.timerfacility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.neozant.request.ScheduleDataRequest;
import com.neozant.request.TimerData;



public class TimerTaskManager {

	final static Logger logger = Logger.getLogger(TimerTaskManager.class);
	
    private String           apiServiceGroupID;
	
	private SchedulerFactory schedulerFactory; 
	
	private Scheduler        timerTaskScheduler;
	
	
	public TimerTaskManager(String apiServiceGroupID) {
		
		this.apiServiceGroupID = apiServiceGroupID;
		this.schedulerFactory      = new StdSchedulerFactory();
		
		try {
			this.timerTaskScheduler    = this.schedulerFactory.getScheduler();
			
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	/**
	 * Lifecycle method for the {@code TimerTaskManager}. Must be called before timer-tasks can be scheduled by it. If timer-tasks are submitted 
	 * to the {@code TimerTaskManager} without starting the scheduler through this call, timer-tasks are silently ignored.
	 */
	public void start () {
		 
		try {
			
			this.timerTaskScheduler.start();
			 logger.info("TimerManager started ....");
			
		} catch (SchedulerException e) {
			 logger.error("ERROR :: Failed to start TimerManager");
			e.printStackTrace(); 
		}
	}
	
	
	/**
	 * Lifecycle method for the {@code TimerTaskManager}. Must be called to stop the scheduler and its associated threads run by the {@code TimerTaskManager} 
	 * before the manager can be disposed off. Failing to call this will keep the threads alive and thus will prevent the JVM from exiting.
	 */
	public void stop () {
		
		try {
			
			this.timerTaskScheduler.shutdown();
			
		} catch (SchedulerException e) {
			logger.error("TimerTaskManager::ERROR :: Failed to stop TimerManager:"+e.getMessage());
			//System.out.println("ERROR :: Failed to stop TimerManager");
			e.printStackTrace();
		}
	}
	
	
	public String scheduleTimerTask (ScheduleDataRequest scheduleData,String callerSuppliedUniqID) throws ParseException {
		
				// We need to create an unique identification for this job before submitting this to the scheduler
				StringBuffer sb = new StringBuffer(callerSuppliedUniqID);     /* assumed to be a unique value passed by the caller */
				
				TimerData timerData=scheduleData.getTimerData();
				
				String dateInString = timerData.getDate()+"-"+timerData.getMonth()+"-"+timerData.getYear()+" "+timerData.getHour()+":"+timerData.getMinutes()+":00 "+timerData.getAmPmMarker();
				
				logger.info("TimerTaskManager::DATE IN STRING WE GET IS::"+dateInString);
				//System.out.println("DATE IN STRING WE GET IS::"+dateInString);
				
				SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss aa");
			    
			    Date date= sdf.parse(dateInString);
				
			   // DATE IN STRING WE GET IS::13-6-2016 48:8:00 pm
			  //TIMER OF THE DATA TO BE FIRED IS::::15-6-2016 12:08:00 PM
			    
			    logger.info("TIMER OF THE DATA TO BE FIRED IS::::"+sdf.format(date));
				
			    //System.out.println("TIMER OF THE DATA TO BE FIRED IS::::"+sdf.format(date));
			    
				JobDataMap jobData = new JobDataMap();
				jobData.put("scheduleData",scheduleData);
				
				JobBuilder jobBuilder = JobBuilder.newJob(TimerTaskToBeFired.class);
				
				JobDetail jobDetail = jobBuilder.usingJobData(jobData)
												.withIdentity("myJob"+callerSuppliedUniqID, "group1"+callerSuppliedUniqID)
												.withDescription("Job is to Fire Task And Create Report")
												.build();
				
				
				StringBuffer trigID = new StringBuffer().append("Trig-")
														.append(sb.toString());
				
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				int hours = calendar.get(Calendar.HOUR_OF_DAY);
				int minutes = calendar.get(Calendar.MINUTE);
				
				Trigger jobTrigger ;
				
				
				switch(timerData.getRepeatOn().toLowerCase()){
				case "daily":
					logger.info("TimerTaskManager::SCHEDULING FOR DAILY TIMER");
					jobTrigger= TriggerBuilder.newTrigger()
					   			.withIdentity(trigID.toString(), this.apiServiceGroupID)
					   			.startAt(date)
					   			.withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(hours,minutes))
					   			.build();
					break;
				case "weekday":
					logger.info("TimerTaskManager::SCHEDULING FOR WEEKDAY TIMER");
					String expressionForWeekdayTimer="0 "+minutes+" "+hours+" ? * MON-SAT";
					jobTrigger= TriggerBuilder.newTrigger()
							    .withIdentity(trigID.toString(), this.apiServiceGroupID)
							    .startAt(date)
							    .withSchedule(CronScheduleBuilder.cronSchedule(expressionForWeekdayTimer))
							    .build();
					break;
				case "onetime":
					logger.info("TimerTaskManager::SCHEDULING FOR ONE TIMER");
					jobTrigger= TriggerBuilder.newTrigger()
		   						.withIdentity(trigID.toString(), this.apiServiceGroupID)
		   						.startAt(date)
		   						.withSchedule(SimpleScheduleBuilder.simpleSchedule())
		   						.build();
				    break;
				default:
					logger.info("TimerTaskManager::SCHEDULING FOR DEFAULT ONE TIMER");
					jobTrigger= TriggerBuilder.newTrigger()
		   						.withIdentity(trigID.toString(), this.apiServiceGroupID)
		   						.startAt(date)
		   						.withSchedule(SimpleScheduleBuilder.simpleSchedule())
		   						.build();
					break;
				}
				
				// [kv]: Finally, we register the job/trigger combination with the scheduler.
				try {
					
					this.timerTaskScheduler.scheduleJob(jobDetail,jobTrigger);
					
				} catch (SchedulerException e) {
					
					logger.error("ERROR :: Failed to schedule job, with ID [" + jobDetail.getDescription() + "]");
					e.printStackTrace();
					return (null);
				} 
				
				logger.info("Scheduled new job, with Key [" + jobDetail.getKey() + "]");
					
				return (trigID == null ? null : jobTrigger.getKey().getName());
	}
	

}
