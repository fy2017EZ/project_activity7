package com.imooc.activitiweb.controller;

import com.imooc.activitiweb.Enum.ColumnTypeEnum;
import com.imooc.activitiweb.SecurityUtil;
import com.imooc.activitiweb.mapper.GoodInfoMapper;
import com.imooc.activitiweb.mapper.UserInfoBeanMapper;
import com.imooc.activitiweb.pojo.CityInfo;
import com.imooc.activitiweb.pojo.GoodInfo;
import com.imooc.activitiweb.pojo.UserInfoBean;
import com.imooc.activitiweb.service.GoodService;
import com.imooc.activitiweb.util.AjaxResponse;
import com.imooc.activitiweb.util.GlobalConfig;
import io.swagger.models.auth.In;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.engine.repository.ProcessDefinition;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import sun.misc.BASE64Encoder;

import javax.sql.rowset.serial.SerialBlob;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/GoodInfo")
public class GoodInfoController {

    @Autowired
    private SecurityUtil securityUtil;
    @Autowired
    private GoodInfoMapper mapper;
    @Autowired
    private UserInfoBeanMapper userInfoBeanMapper;
    @Autowired
    private GoodService goodService;
    //查询所有医疗物品清单
    @GetMapping(value = "/getAllGoodInfo")
    public AjaxResponse getAllGoodInfo(@AuthenticationPrincipal UserInfoBean userInfoBean,
                                       @RequestParam(value = "userName")String userName) {
        List<GoodInfo> list = null;
        try {
            //测试用写死的用户POSTMAN测试用；生产场景已经登录，在processDefinitions中可以获取到当前登录用户的信息
//            if (GlobalConfig.Test) {
//                securityUtil.logInAs("wukong");
//            }
        //获取当前登录用户角色
            UserInfoBean infoBean = userInfoBeanMapper.selectByUsername(userName);
            if(infoBean.getCityId().equals("999")){//管理员登录
                list = mapper.getAllGoodInfo();
            }else {//地市处理人
                list = mapper.getCityGoodInfo(userInfoBean.getCityId());
            }

            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(),list);
        } catch (Exception e) {
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取信息失败", e.toString());
        }

    }
    //根据条件查询医疗物品
    @GetMapping("/getMedicalInfo")
    public AjaxResponse getMedicalInfo(@RequestParam(value = "beginTime",required = false)String beginTime,
                                       @RequestParam(value = "endTime",required = false)String endTime,
                                       @RequestParam(value = "category",required = false)String category,
                                       @RequestParam(value = "goodName",required = false)String goodName,
                                       @RequestParam(value = "maxNums",required = false)String maxNums,
                                       @RequestParam(value = "minNums",required = false)String minNums,
                                       @RequestParam(value = "userName",required = false)String userName){
        try{
            List<GoodInfo> allGoodInfo = null;
            String columnValue = "";
            if(StringUtils.isEmpty(beginTime)&&StringUtils.isEmpty(endTime)&&StringUtils.isEmpty(category)&&
                    StringUtils.isEmpty(goodName)&&StringUtils.isEmpty(maxNums)&&StringUtils.isEmpty(minNums)){
                allGoodInfo = mapper.getAllGoodInfo();
            }
            if(!StringUtils.isEmpty(beginTime)&&!StringUtils.isEmpty(endTime)){
                if(columnValue.equals("")){
                    columnValue = " where start_time between date_format('"+beginTime+"','%Y-%m-%d') and date_format('"+endTime+"','%Y-%m-%d')";
                }else{
                    columnValue = columnValue+" and start_time between date_format('"+beginTime+"','%Y-%m-%d') and date_format('"+endTime+"','%Y-%m-%d')";
                }
            }
            if(!StringUtils.isEmpty(minNums)&&!StringUtils.isEmpty(maxNums)){
                if(columnValue.equals("")){
                    columnValue = " where nums between "+minNums+" and "+maxNums;
                }else{
                    columnValue = columnValue+" and nums between "+minNums+" and "+maxNums;
                }
            }
            if(!StringUtils.isEmpty("goodName")){
                if(columnValue.equals("")){
                    columnValue = " where good_name like '%"+goodName+"%'";
                }else{
                    columnValue = columnValue+" and good_name like '%"+goodName+"%'";;
                }
            }
            if(!StringUtils.isEmpty(category)){
                if(columnValue.equals("")){
                    columnValue = " where category = "+category;
                }else{
                    columnValue = columnValue+" category = "+category;;
                }
            }
            if((!StringUtils.isEmpty(beginTime)&&StringUtils.isEmpty(endTime))||(!StringUtils.isEmpty(minNums)
                    &&StringUtils.isEmpty(maxNums))){
                return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                        "调用有误：", "时间/数量范围不全");
            }
            System.out.println("sql:"+columnValue);
            if(userName.equals("admin")){
                allGoodInfo = mapper.getMedicalInfo(columnValue);
            }else{
                UserInfoBean userInfoBean = userInfoBeanMapper.selectByUsername(userName);
                if(StringUtils.isEmpty(beginTime)&&StringUtils.isEmpty(endTime)&&StringUtils.isEmpty(category)&&
                        StringUtils.isEmpty(goodName)&&StringUtils.isEmpty(maxNums)&&StringUtils.isEmpty(minNums)){
                    allGoodInfo = mapper.getCityGoodInfo(userInfoBean.getCityId());
                }else{
                    allGoodInfo = mapper.getCityMedicalInfo(columnValue,userInfoBean.getCityId());
                }

            }
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(),allGoodInfo);
        }catch (Exception e){
            e.printStackTrace();
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取信息失败", e.toString());
        }

    }
    @PostMapping("/uploadFile")
    public AjaxResponse uploadFile(@RequestParam(value = "userName",required = false)String userName,
                                   @RequestParam(value = "goodId",required = false)String goodId,
                                   @RequestParam(value = "file",required = true) MultipartFile file){
        try {
            goodService.uploadImg(userName,goodId,file);
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(),"上传成功");
        }catch (Exception e){
            e.printStackTrace();
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取信息失败", e.toString());
        }
    }
    @PostMapping("/uploadExecl")
    public AjaxResponse uploadFile(@RequestParam(value = "userName",required = false)String userName,
                                   @RequestParam(value = "file",required = true) MultipartFile file){
        try {
            goodService.uploadExecl(userName,file);
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(),null);
        }catch (Exception e){
            e.printStackTrace();
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取信息失败", e.toString());
        }
    }
    @PostMapping("/addMedicalInfo")
    public AjaxResponse addMedicalInfo(@RequestParam(value = "goodName",required = true)String goodName,
                                       @RequestParam(value = "price",required = false)String price,
                                       @RequestParam(value = "quality",required = false)String quality,
                                       @RequestParam(value = "nums",required = true)Integer nums,
                                       @RequestParam(value = "category",required = false)String category,
                                       @RequestParam(value = "company",required = false)String company,
                                       @RequestParam(value = "message",required = false)String message){
        try {
            int id = (int) (Math.random() * 1000000 + 1);
            String pId = UUID.randomUUID().toString();
            GoodInfo info = new GoodInfo();
            info.setGood_id(Integer.toUnsignedLong(id));
            info.setGood_name(goodName);
            info.setPrice(price);
            info.setQuality(quality);
            info.setNums(nums);
            info.setCategory(category);
            info.setCompany(company);
            info.setMessage(message);
            info.setWarehousing_batch_number(pId);
            mapper.insertGoodInfo(info);
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(),null);
        }catch (Exception e){
            e.printStackTrace();
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取信息失败", e.toString());
        }
    }
    //派发
    @PostMapping("/sendMedicalInfo")
    public AjaxResponse sendMedicalInfo(@RequestParam(value = "goodIds",required = true)Object[] goodIds,
                                       @RequestParam(value = "userName",required = true)String userName,
                                       @RequestParam(value = "cityId",required = true)String cityId,
                                       @RequestParam(value = "nums",required = true) Integer nums){
        try {
                if(!userName.equals("admin")&&cityId.equals("999")){
                    return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                            "用户权限不足", null);
                }
                for (String goodId:(String[])goodIds) {
                    GoodInfo info = mapper.selectGoodInfoById(goodId);
                    if(cityId.equals("999")){//全省
                    if(info.getNums()<nums*11){
                        return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                                "实际物资数小于派发数总和", null);
                    }else{
                        List<CityInfo> cityInfo = mapper.getAllCityInfo();
                        for (CityInfo city:cityInfo) {
                            info.setNums(nums);
                            info.setCityId(cityId);
                            mapper.updateCityGoodInfo(info);
                        }
                    }
                    }else{
                        if(info.getNums()<nums){
                            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                                    "实际物资数小于派发数总和", null);
                        }else{
                            info.setNums(nums);
                            info.setCityId(cityId);
                            mapper.updateCityGoodInfo(info);
                        }
                    }
                 }
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(),null);
        }catch (Exception e){
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取信息失败", e.toString());
        }
    }
    //转派
    @PostMapping("/sendCityMedicalInfo")
    public AjaxResponse sendCityMedicalInfo(@RequestParam(value = "goodIds",required = true)String[] goodIds,
                                        @RequestParam(value = "cityId",required = true)String cityId,
                                        @RequestParam(value = "targetCityId",required = true)String targetCityId,
                                        @RequestParam(value = "nums",required = true) Integer nums){
        try {

            for (String goodId:goodIds) {
                GoodInfo info = mapper.selectCityGoodInfoById(goodId,cityId);
                GoodInfo info1 = mapper.selectCityGoodInfoById(goodId, targetCityId);
                if(info.getNums()<nums){
                        return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                                "实际物资数小于派发数总和", null);
                    }else{
                        info.setNums(info.getNums()-nums);
                        info.setCityId(cityId);
                        mapper.updateCityGoodInfo(info);
                        info.setNums(StringUtils.isEmpty(info1.getNums())?nums:info1.getNums()+nums);
                        info.setCityId(targetCityId);
                        mapper.updateCityGoodInfo(info);
                    }
                }
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(),null);
        }catch (Exception e){
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取信息失败", e.toString());
        }
    }
    @GetMapping("/selectMedicalInfoById")
    public AjaxResponse selectMedicalInfoById(@RequestParam(value = "goodId",required = true)String goodId,
                                              @RequestParam(value = "cityId",required = false)String cityId){
        try {
            GoodInfo info = null;
            String tableName = "";
            if(StringUtils.isEmpty(cityId)||cityId.equals("999")){
                info = mapper.selectGoodInfoById(goodId);
                tableName = "medical_good_info";
            }else{
                info = mapper.selectCityGoodInfoById(goodId, cityId);
                tableName = "medical_"+cityId+"_good_info";
            }
            if(StringUtils.isEmpty(mapper.getSuffix(tableName,goodId))){
            }else{
                JSONObject jsonObject = goodService.readImgUrl(null, goodId);
                info.setImg_url((String)jsonObject.get("imgData")==null?"":(String)jsonObject.get("imgData"));
                info.setImg_suffix((String) jsonObject.get("suffix")==null?"":(String) jsonObject.get("suffix"));
            }

            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(),info);
        }catch (Exception e){
            e.printStackTrace();
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取信息失败", e.toString());
        }
    }

    @GetMapping("/deleteMedicalInfoById")
    public AjaxResponse deleteMedicalInfoById(@RequestParam(value = "goodId",required = true)String goodId,
                                              @RequestParam(value = "cityId",required = false)String cityId){
        try {
            if(StringUtils.isEmpty(cityId)||cityId.equals("999")){//删除全省药品
                List<CityInfo> cityInfo = mapper.getAllCityInfo();
                mapper.deleteGoodInfo(goodId);
                for (CityInfo city:cityInfo) {
                    mapper.deleteCityGoodInfo(goodId,city.getCity_id());
                }
            }else{
                GoodInfo info = mapper.selectCityGoodInfoById(goodId, cityId);
                mapper.deleteCityGoodInfo(goodId,cityId);
                GoodInfo info1 = mapper.selectGoodInfoById(goodId);
                mapper.updateGoodInfoNums(info1.getNums()-info.getNums(),goodId);
            }
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(),"操作成功");
        }catch (Exception e){
            e.printStackTrace();
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取信息失败", e.toString());
        }
    }
    @PostMapping("/updateMedicalInfo")
    public AjaxResponse updateMedicalInfo(@RequestParam(value = "goodId",required = true)String goodId,
                                       @RequestParam(value = "goodName",required = false)String goodName,
                                       @RequestParam(value = "price",required = false)String price,
                                       @RequestParam(value = "quality",required = false)String quality,
                                       @RequestParam(value = "nums",required = false)Integer nums,
                                       @RequestParam(value = "startTime",required = false)String startTime,
                                       @RequestParam(value = "category",required = false)String category,
                                       @RequestParam(value = "company",required = false)String company,
                                       @RequestParam(value = "message",required = false)String message,
                                          @RequestParam(value = "pId",required = false)String pId){
        try {
            GoodInfo info = new GoodInfo();
            info.setGood_id(Long.parseLong(goodId));
            info.setGood_name(goodName);
            info.setPrice(price);
            info.setQuality(quality);
            info.setNums(nums);
            info.setCategory(category);
            info.setCompany(company);
            info.setMessage(message);
            mapper.updateGoodInfo(info);
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.SUCCESS.getCode(),
                    GlobalConfig.ResponseCode.SUCCESS.getDesc(),null);
        }catch (Exception e){
            return AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                    "获取信息失败", e.toString());
        }
    }
}








