package com.example.webrtc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebRTCPageController {

    @RequestMapping("/index")
    public String index(){
        return "index";
    }
}
