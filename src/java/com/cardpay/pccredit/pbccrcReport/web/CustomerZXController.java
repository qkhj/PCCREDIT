package com.cardpay.pccredit.pbccrcReport.web;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cardpay.pccredit.pbccrcReport.model.RH_INFO;
import com.cardpay.pccredit.pbccrcReport.service.RhzxService;
import com.cardpay.pccredit.pbccrcReport.util.DateUtil;
import com.cardpay.pccredit.pbccrcReport.util.PbccrcReport;
import com.cardpay.pccredit.pbccrcReport.util.PbocFileReader;
import com.cardpay.pccredit.customer.filter.CustomerInforFilter;
import com.cardpay.pccredit.customer.model.CustomerInfor;
import com.cardpay.pccredit.customer.model.CustomerMainManager;
import com.cardpay.pccredit.customer.service.CustomerInforService;
import com.cardpay.pccredit.customer.service.CustomerMainManagerService;
import com.cardpay.pccredit.product.service.ProductService;
import com.wicresoft.jrad.base.auth.IUser;
import com.wicresoft.jrad.base.auth.JRadModule;
import com.wicresoft.jrad.base.auth.JRadOperation;
import com.wicresoft.jrad.base.database.model.QueryResult;
import com.wicresoft.jrad.base.web.JRadModelAndView;
import com.wicresoft.jrad.base.web.controller.BaseController;
import com.wicresoft.jrad.base.web.result.JRadPagedQueryResult;
import com.wicresoft.jrad.base.web.result.JRadReturnMap;
import com.wicresoft.jrad.base.web.security.LoginManager;
import com.wicresoft.jrad.base.web.utility.WebRequestHelper;
import com.wicresoft.jrad.modules.privilege.service.UserService;
import com.wicresoft.util.spring.Beans;
import com.wicresoft.util.spring.mvc.mv.AbstractModelAndView;

/**
 * CustomerZXController类的描述
 * 
 * @author 谭文华
 * @created on 2015-01-13
 * 
 * @version $Id:$
 */
@Controller
@RequestMapping("/customer/customerzx/*")
@JRadModule("customer.customerzx")
public class CustomerZXController extends BaseController {

	Logger logger = Logger.getLogger(this.getClass());
	
	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CustomerInforService customerInforService;

	@Autowired
	private CustomerMainManagerService customerMainManagerService;

	@Autowired
	private RhzxService rhzxService;
	
	/**
	 * 设置征信查询条件
	 */
	@ResponseBody
	@RequestMapping(value = "conditionRhzx.page", method = { RequestMethod.GET })
	@JRadOperation(JRadOperation.RHZX)
	public AbstractModelAndView conditionRhzx(HttpServletRequest request) {
		String customerId = request.getParameter("customerId");
		CustomerInfor customerInfor = customerInforService.findCustomerInforById(customerId);
		JRadModelAndView mv = new JRadModelAndView("/customer/customerzx/customerzx_condition",request);
		mv.addObject("customerInfor", customerInfor);
		return mv;
	}
	
	/**
	 * 查询人行征信
	 * 
	 * @param filter
	 * @param request
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "checkRhzx.page", method = { RequestMethod.POST })
	@JRadOperation(JRadOperation.RHZX)
	public AbstractModelAndView checkRhzx(HttpServletRequest request) {
		try {
			request.setCharacterEncoding("utf8");
			String customerId = request.getParameter("customerId");
			String QueryReason = request.getParameter("QueryReason");
			String QueryType = request.getParameter("QueryType");
			String Vertype = request.getParameter("Vertype");
			
			CustomerInfor customer = customerInforService.findCustomerInforById(customerId);
			IUser user = Beans.get(LoginManager.class).getLoggedInUser(request);
			logger.info("人行征信查询---查询用户:"+user.getId()+",客户:"+customer.getChineseName());
			PbccrcReport pbccrcReport = new PbccrcReport();
			RH_INFO rh_info = this.rhzxService.findByCustomerId(customerId);
			//不存在或大于30天时重新抓征信
			if(rh_info == null || DateUtil.daysBetween(rh_info.getModifiedTime(), new Date())>30){
				//备份征信htm文件到服务器
				String fileFullPath = pbccrcReport.manuProcessPbocCreditInfo(customer.getChineseName(), customer.getCardType(), customer.getCardId(),
						QueryReason,QueryType,Vertype);
				if(fileFullPath != null){
					//解析htm文件抓取内容并存入数据库
					this.rhzxService.insertOrUpdateZX(customerId, fileFullPath);
				}
			}
			JRadModelAndView mv = new JRadModelAndView("/customer/customerzx/rhzx/"+customer.getChineseName()+"_"+customer.getCardId(), request);
			mv.addObject("customer", customer);
			return mv;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JRadModelAndView mv = new JRadModelAndView("/customer/customerzx/rhzx/error", request);
		return mv;
	}
}
