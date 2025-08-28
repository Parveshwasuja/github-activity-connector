package com.example.demo.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Repository;
import com.example.demo.service.GithubService;

@RestController
@RequestMapping("/api/github")
public class DemoController {
	
	@Autowired
	private GithubService githubService;
	
	@GetMapping("/repos/{username}")
	public List<Repository> giveHello(@PathVariable String username, @RequestHeader("Authorization") String token) throws IOException {
		return githubService.fetchRepositories(username, token);
	}

}
