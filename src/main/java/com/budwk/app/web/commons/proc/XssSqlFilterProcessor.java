package com.budwk.app.web.commons.proc;

import com.budwk.app.base.result.Result;
import com.budwk.app.base.utils.WebUtil;
import org.apache.commons.lang3.StringUtils;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.lang.Strings;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.mvc.ActionContext;
import org.nutz.mvc.ActionInfo;
import org.nutz.mvc.Mvcs;
import org.nutz.mvc.NutConfig;
import org.nutz.mvc.impl.processor.AbstractProcessor;
import org.nutz.mvc.view.ForwardView;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * SQL XSS拦截
 * Created by wizzer on 2016/7/1.
 */
public class XssSqlFilterProcessor extends AbstractProcessor {

    private static final Log log = Logs.get();
    protected String lerrorUri = "/error/403.html";
    private PropertiesProxy conf;
    private List<String> ignoreList;

    @Override
    public void init(NutConfig config, ActionInfo ai) throws Throwable {
        try {
            conf = config.getIoc().get(org.nutz.ioc.impl.PropertiesProxy.class, "conf");
            ignoreList = Arrays.asList(Strings.splitIgnoreBlank(conf.get("xsssql.ignore.urls", "")));
        } catch (Exception e) {
        }
    }

    public void process(ActionContext ac) throws Throwable {
        if (checkUrl(ac) && checkParams(ac)) {
            if (WebUtil.isAjax(ac.getRequest())) {
                ac.getResponse().addHeader("loginStatus", "paramsDenied");
                WebUtil.rendAjaxResp(ac.getRequest(), ac.getResponse(), Result.error(Mvcs.getMessage(ac.getRequest(), "system.paramserror")));
            } else {
                new ForwardView(lerrorUri).render(ac.getRequest(), ac.getResponse(), Mvcs.getMessage(ac.getRequest(), "system.paramserror"));
            }
            return;
        }
        doNext(ac);
    }

    private boolean checkUrl(ActionContext ac) {
        String path = ac.getPath();
        return !ignoreList.contains(path);
    }

    protected boolean checkParams(ActionContext ac) {
        HttpServletRequest req = ac.getRequest();
        Iterator<String[]> values = req.getParameterMap().values().iterator();// 获取所有的表单参数
        Iterator<String[]> values2 = req.getParameterMap().values().iterator();// 因为是游标所以要重新获取
        boolean isError = false;
        String regEx_sql = "select|update|and|or|delete|insert|trancate|char|chr|into|substr|ascii|declare|exec|count|master|drop|execute";
        String regEx_xss = "script|iframe";
        //SQL过滤
        while (values.hasNext()) {
            String[] valueArray = (String[]) values.next();
            for (int i = 0; i < valueArray.length; i++) {
                String value = valueArray[i].toLowerCase();
                //分拆关键字
                String[] inj_stra = StringUtils.split(regEx_sql, "|");
                for (int j = 0; j < inj_stra.length; j++) {
                    // 判断如果路径参数值中含有关键字则返回true,并且结束循环
                    if ("and".equals(inj_stra[j]) || "or".equals(inj_stra[j]) || "into".equals(inj_stra[j])) {
                        if (value.contains(" " + inj_stra[j] + " ")) {
                            isError = true;
                            log.debugf("[%-4s]URI=%s %s", req.getMethod(), req.getRequestURI(), "SQL关键字过滤:" + value);
                            break;
                        }
                    } else {
                        if (value.contains(" " + inj_stra[j] + " ")
                                || value.contains(
                                inj_stra[j] + " ")) {
                            isError = true;
                            log.debugf("[%-4s]URI=%s %s", req.getMethod(), req.getRequestURI(), "SQL关键字过滤:" + value);
                            break;
                        }
                    }
                }
                if (isError) {
                    break;
                }
            }
            if (isError) {
                break;
            }
        }
        if (!isError) {
            // XSS漏洞过滤
            while (values2.hasNext()) {
                String[] valueArray = (String[]) values2.next();
                for (int i = 0; i < valueArray.length; i++) {
                    String value = valueArray[i].toLowerCase();
                    // 分拆关键字
                    String[] inj_stra = StringUtils.split(regEx_xss, "|");
                    for (int j = 0; j < inj_stra.length; j++) {
                        // 判断如果路径参数值中含有关键字则返回true,并且结束循环
                        if (value.contains("<" + inj_stra[j] + ">")
                                || value.contains("<" + inj_stra[j])
                                || value.contains(inj_stra[j] + ">")) {
                            log.debugf("[%-4s]URI=%s %s", req.getMethod(), req.getRequestURI(), "XSS关键字过滤:" + value);
                            isError = true;
                            break;
                        }
                    }
                    if (isError) {
                        break;
                    }
                }
                if (isError) {
                    break;
                }
            }
        }
        return isError;
    }
}
