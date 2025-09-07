package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(DemoApplication.class, args);
	}
	
//	 If this collection is used for SOME USE CASE (for example, converting to JIRA issues for multiple users using different JIRA systems). How will you change the design?

	// Change in design : 
//	Track Sync Status per User + JIRA Instance
//	Store sync metadata (status, timeStamps, JIRA issue keys) for each GitHub issue per user and per JIRA system to avoid duplicates and enable retries.
//
//	MultiTenancy Support
//	Separate data and credentials by user/JIRA instance to securely handle multiple users with different JIRA setups.
//	
//	Event-Driven Sync
//	Use events (e.g., FireStore triggers or message queues) to asynchronously process and sync issues to JIRA for Scalability and Reliability.

}
