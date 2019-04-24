package com.example.demo.service;

import com.example.demo.service.impl.IDemoService;
import com.example.mvcframework.annotation.SWService;

/**
 * @author SW
 * @date create 2019-04-24 18:11
 */
@SWService
public class DemoServer implements IDemoService {

    public String get(String name) {
        return name;
    }
}
