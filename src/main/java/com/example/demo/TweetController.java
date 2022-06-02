package com.example.demo;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TweetController {

    @RequestMapping("/tweets")
    public String index() {
        return "Hello Spring Boot!";
    }

}
