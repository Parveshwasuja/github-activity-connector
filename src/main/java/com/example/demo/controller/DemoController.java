package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.SyncService;

@RestController
@RequestMapping("/api/github")
public class DemoController {
	
	@Autowired
	private SyncService syncService;
	
	@GetMapping("/repos/{owner}/{repo}")
	public String giveHello(@PathVariable String owner, @PathVariable String repo) throws Exception {
		syncService.syncIssues(owner, repo);
		return "Successful response";
	}
}
