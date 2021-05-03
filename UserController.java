package com.imooc.activitiweb.controller;

import com.imooc.activitiweb.mapper.ActivitiMapper;
import com.imooc.activitiweb.pojo.UserInfoBean;
import com.imooc.activitiweb.service.GoodService;
import com.imooc.activitiweb.util.AjaxResponse;
import com.imooc.activitiweb.util.GlobalConfig;
import org.activiti.engine.impl.util.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    ActivitiMapper mapper;

    @Autowired
    GoodService service;

    //获取用户
    @GetMapping(value = "/getUsers")
    public AjaxResponse getUsers() {
        try {
            List<HashMap<String, Object>> userList = mapper.selectUser();
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), userList);


        } catch (Exception e) {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取用户列表失败", e.toString());
        }
    }
    //获取用户
    @GetMapping(value = "/getUsersByUserName")
    public AjaxResponse getUsersByUserName(@RequestParam(value = "username",required = true)String username) {
        try {
            List<UserInfoBean> userInfoBean = mapper.getUserByUserName(username);
//            JSONObject jsonObject = service.readImgUrl(username, null);
//            userInfoBean.get(0).setImg_url((String)jsonObject.get("imgData"));
//            userInfoBean.get(0).setImg_suffix((String)jsonObject.get("suffix"));
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), userInfoBean.get(0));


        } catch (Exception e) {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取用户列表失败", e.toString());
        }
    }
    //注册用户
    @PostMapping("/addUser")
    public AjaxResponse addUser(@RequestParam(value = "name",required = false)String name,
                                @RequestParam(value = "address",required = false)String address,
                                @RequestParam(value = "username",required = true)String username,
                                @RequestParam(value = "password",required = true)String password,
                                @RequestParam(value = "roles",required = false)String roles,
                                @RequestParam(value = "cityId",required = true)String cityId){
        try {
            List<UserInfoBean> userList = mapper.getUserByUserName(username);
            if(userList.isEmpty()){
                int id = (int)(Math.random()*1000000+1);
                String passWord=passwordEncoder().encode(password);
                UserInfoBean userInfoBean = new UserInfoBean();
            }


            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(), userList);


        } catch (Exception e) {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "注册用户信息失败", e.toString());
        }
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
