package com.shubham.ratelimiter;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {


    @GetMapping("/users")
    @RateLimit(limit = 100, windowsSeconds = 60)
    public String getUser(){
        return "Shubham";
    }

}
