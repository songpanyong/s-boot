package com.guohuai.boot.pay.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONObject;
import com.guohuai.basic.common.StringUtil;
import com.guohuai.boot.PayEnum;
import com.guohuai.boot.pay.dao.ComOrderDao;
import com.guohuai.boot.pay.dao.PaymentDao;
import com.guohuai.boot.pay.dao.ReconciliationPassDao;
import com.guohuai.boot.pay.form.ReconciliationRecordsForm;
import com.guohuai.boot.pay.res.ReconciliationPassVoRes;
import com.guohuai.boot.pay.vo.OrderVo;
import com.guohuai.boot.pay.vo.PaymentVo;
import com.guohuai.boot.pay.vo.ReconciliationPassVo;
import com.guohuai.boot.pay.vo.ReconciliationRecordsVo;
import com.guohuai.component.util.Constant;
import com.guohuai.component.util.TradeTypeEnum;
import com.guohuai.payadapter.component.TradeChannel;
import com.guohuai.payadapter.listener.event.ReconciliationPassEvent;

@Service
public class ReconciliationPassService {
	private final static Logger log = LoggerFactory.getLogger(ReconciliationPassService.class);
	@Autowired
	private ReconciliationPassDao reconciliationPassDao;
	@Autowired
	private PaymentDao paymentDao;
	@Autowired
	private ComOrderDao comOrderDao;
	@Autowired
	private ReconciliationRecordsService reconciliationRecordsService;
	@Autowired
	ApplicationEventPublisher publisher;
	
	@Value("${payadapter_environment}")
	private String environment;
	@Value("${seq.env}")
	private String seqEnv;
	@Value("${baofoo.memberId:100000178}")
	String memberId;
	
	public void reconciliation() {
		// ---以指令表为基础比较快付通表数据(查询没有对过帐的数据)
		List<PaymentVo> payLists = paymentDao.findByDiffPass(TradeTypeEnum.trade_pay.getCode());
		List<PaymentVo> mentNos = paymentDao.findByNo(TradeTypeEnum.trade_pay.getCode());
		List<ReconciliationPassVo> reconNos = reconciliationPassDao.findByNo();
		List<OrderVo> orderNos = comOrderDao.findByNos(TradeTypeEnum.trade_pay.getCode());

		if (payLists.isEmpty() && (!mentNos.isEmpty())) {
			nii(mentNos, reconNos, orderNos);
		} else if (!payLists.isEmpty()) {
			nii(mentNos, reconNos, orderNos, payLists);
		}
	}

	public void nii(List<PaymentVo> mentNos, List<ReconciliationPassVo> reconNos, List<OrderVo> orderNos) {
		Timestamp time = new Timestamp(System.currentTimeMillis());

		// --多单(修改状态为废单--收单、指令、交互、账户余额减少)【对支付指令表数据修改废单】
		log.info("快付通支付,多单,{}条对账", mentNos.size());
		for (PaymentVo vo : mentNos) {
			for (OrderVo orderVo : orderNos) {
				if (vo.getOrderNo().trim().equals(orderVo.getOrderNo().trim())) {
					orderVo.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION3.getCode()));
					orderVo.setUpdateTime(time);
					if (orderVo.getStatus().equals(PayEnum.PAY1.getCode())) {
						orderVo.setStatus(PayEnum.PAY2.getCode());
					}
				}
			}
			vo.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION3.getCode()));
			vo.setUpdateTime(time);
			if (vo.getCommandStatus().equals(PayEnum.PAY1.getCode())) {
				vo.setCommandStatus(PayEnum.PAY2.getCode());
				// ---账户余额增加
			}

			// 补单

		}
		paymentDao.save(mentNos);
		comOrderDao.save(orderNos);
	}

	public void nii(List<PaymentVo> mentNos, List<ReconciliationPassVo> reconNos, List<OrderVo> orderNos,
			List<PaymentVo> payLists) {
		Timestamp time = new Timestamp(System.currentTimeMillis());
		Map<String, PaymentVo> maps = new HashMap<String, PaymentVo>();

		// ---修改公共的数据状态
		log.info("快付通支付,公共单,{}条对账", payLists.size());
		for (PaymentVo vo : payLists) {
			for (ReconciliationPassVo recon : reconNos) {
				if (vo.getPayNo().trim().equals(recon.getOrderId().trim())) {
					if (!vo.getCommandStatus().trim().equals(recon.getTradStatus().trim())) {
						vo.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION2.getCode()));
						recon.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION2.getCode()));
						if (vo.getCommandStatus().trim().equals(PayEnum.PAY1.getCode())) {
							// 账户余额减少

						} else if (recon.getTradStatus().trim().equals(PayEnum.PAY1.getCode())) {
							// 账户余额增加
						}
						String status = StringUtil.isEmpty(PayEnum.getName(recon.getTradStatus().trim()))
								? PayEnum.PAY4.getCode() : recon.getTradStatus().trim();
						vo.setCommandStatus(status);
					} else {
						vo.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION1.getCode()));
						recon.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION1.getCode()));
					}
					recon.setUpdateTime(time);
					vo.setUpdateTime(time);
					maps.put(vo.getOrderNo(), vo);
					break;
				}
			}
		}

		// --多单(修改状态为废单--收单、指令、交互、账户余额减少)【对支付指令表数据修改废单】
		if (mentNos.size() > reconNos.size()) {
			log.info("快付通支付,公共单,多单对账");
			for (PaymentVo vo : mentNos) {
				if (!maps.containsKey(vo.getOrderNo())) {
					if (vo.getCommandStatus().equals(PayEnum.PAY1.getCode())) {
						vo.setCommandStatus(PayEnum.PAY2.getCode());
						// 账户余额减少
					}
					vo.setReconStatus(Integer.valueOf(PayEnum.RECONCILIATION3.getCode()));
					vo.setUpdateTime(time);
					payLists.add(vo);
				}
			}

			for (PaymentVo vo : payLists) {
				for (OrderVo orderVo : orderNos) {
					if (orderVo.getOrderNo().trim().equals(vo.getOrderNo().trim())) {
						orderVo.setStatus(vo.getCommandStatus());
						orderVo.setReconStatus(vo.getReconStatus());
						orderVo.setUpdateTime(time);
						break;
					}
				}
			}

			comOrderDao.save(orderNos);
			paymentDao.save(payLists);
			reconciliationPassDao.save(reconNos);
		}

	}

	/**
	 * 对账结果查询
	 * 
	 * @param startDate
	 * @param endDate
	 * @param reconStatus
	 * @return
	 */
	public ReconciliationPassVoRes page(String channelId, Timestamp startDate, Timestamp endDate, int reconStatus,
			int page, int row) {
		log.info("{},对账订单查询,{},", startDate, endDate);
		List<ReconciliationPassVo> list;
		if (reconStatus == 4) {
			list = reconciliationPassDao.findByDate(startDate, endDate, channelId);
		} else {
			list = reconciliationPassDao.findByDateAndStatus(startDate, endDate, reconStatus, channelId);
		}
		ReconciliationPassVoRes resp = new ReconciliationPassVoRes();

		int i = 0;
		List<ReconciliationPassVo> pcrow = new ArrayList<ReconciliationPassVo>();// 满足条件后的指定页数的列数的数据
		for (ReconciliationPassVo vo : list) {
			if (i >= (page - 1) * row && i < page * row) {
				pcrow.add(vo);
			}
			i++;
		}
		resp.setPage(page);
		resp.setRow(row);
		resp.setRows(pcrow);
		resp.setTotal(list.size());
		return resp;
	}

	/**
	 * 金运通读取对账文件
	 */
	public Map<String, String> uploadJytRecon(String checkDate,String channelId, MultipartFile file) {
		Map<String, String> returnMap = new HashMap<String, String>();
		// 读取txt文件并入库
		returnMap = JYTgetReconAndInstall(returnMap, channelId, file,checkDate);
		// 记录导入记录
		ReconciliationRecordsForm form = new ReconciliationRecordsForm();
		form.setReconDate(checkDate);
		form.setChannelId(channelId);// 新增字段渠道
		if ("SUCCESS".equals(returnMap.get("responseCode"))) {// 导入成功
			form.setReconStatus("1");// 导入成功状态
			reconciliationRecordsService.save(form);
		}
		return returnMap;
	}

	/**
	 * 金运通解析对账文件入库
	 * @param returnMap
	 * @param upFile
	 * @return
	 */
	@SuppressWarnings("resource")
	public Map<String, String> JYTgetReconAndInstall(Map<String, String> returnMap, String channelId,MultipartFile file,String checkDate) {
		List<ReconciliationPassVo> passList = new ArrayList<ReconciliationPassVo>();
		try {
			log.info("金运通解析对账文件");
			InputStream is = null;
			HSSFWorkbook hssfWorkbook = null;
			if (!file.isEmpty()) {
				is = file.getInputStream();
				hssfWorkbook = new HSSFWorkbook(is);
				for (int numSheet = 0; numSheet < hssfWorkbook.getNumberOfSheets(); numSheet++) {
					HSSFSheet hssfSheet = hssfWorkbook.getSheetAt(numSheet);
					if (hssfSheet == null) {
						continue;
					}
					
					String txtDate = hssfSheet.getRow(2).getCell(0).toString();//第三行第一列,对账时间;
					txtDate = txtDate.replaceAll("-", "");
					log.info("导入时间:"+checkDate+"交易时间:"+txtDate);
					if(!checkDate.equals(txtDate)){
						returnMap.put("responseCode", "FAIL");
						returnMap.put("responseMsg", "导入时间与文件内交易时间不符");
						return returnMap;
					}
					
					for (int rowNum = 4; rowNum <= hssfSheet.getLastRowNum(); rowNum++) {//从第五行开始读取
						HSSFRow hssfRow = hssfSheet.getRow(rowNum);
						if (hssfRow != null) {
							ReconciliationPassVo passVo = new ReconciliationPassVo();
							Date date = new Date();
							Timestamp creatDate = new Timestamp(date.getTime());
							passVo.setCreateTime(creatDate);// 创建时间
							// 代付和代收公共字段
							String tradType = hssfRow.getCell(0).toString();// 交易类型
							DateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							if ("实名支付".equals(tradType) && "8".equals(channelId)) {
								log.info("实名支付文件解析……");
								// 交易类型 订单提交时间 订单完成时间 商户订单号 实名支付流水号 交易金额 币种
								// 客户交易银行账号
								// 客户姓名 交易结果码 交易结果描述 手续费
//								String flowNo = hssfRow.getCell(4).toString();// 实名支付流水号
								
								// 代付和代收公共字段
								Date tradTime = sd.parse(hssfRow.getCell(1).toString());// 订单提交时间,用于对账
								java.sql.Date accountDate = new java.sql.Date(
										sd.parse(hssfRow.getCell(2).toString()).getTime());// 订单完成时间
								String payNo = hssfRow.getCell(3).toString();// 商户订单号
								passVo.setTransactionTime(new Timestamp(tradTime.getTime()));
								passVo.setAccountDate(accountDate);
								
								BigDecimal amount = new BigDecimal(hssfRow.getCell(5).toString());// 交易金额
								String currency = hssfRow.getCell(6).toString();//币种
								String paymentBankNo = hssfRow.getCell(7).toString();// 客户银行帐号
								String tradStatus = hssfRow.getCell(9).toString();// 交易结果
								String failDetail = hssfRow.getCell(10).toString();// 交易结果描述
								String feeStr = hssfRow.getCell(11).toString();//手续费
								if("0".equals(feeStr)){
									feeStr = feeStr+".00";
								}
//								BigDecimal fee = new BigDecimal(feeStr);// 手续费
								
								passVo.setChannelId(channelId);// 渠道号
								passVo.setOrderId(payNo);//商户订单号
								passVo.setTransactionAmount(amount);
								passVo.setTransactionCurrency(currency);
								passVo.setPaymentBankNo(paymentBankNo);
								passVo.setFailDetail(failDetail);
								if ("S0000000".equals(tradStatus)) {
									passVo.setTradStatus("1");
								} else {
									passVo.setTradStatus("2");
								}
							} else if ("代付".equals(tradType) && "7".equals(channelId)) {
								log.info("代付文件解析……");
								// 交易类型 订单提交时间 订单完成时间 商户订单号 批次号 代收付平台订单号 交易金额 币种
								// 客户交易银行账号 客户交易银行账户名称 商户交易账号 交易结果码 交易结果描述 手续费
								// 备注
								
								// 代付和代收公共字段
								Date tradTime = sd.parse(hssfRow.getCell(1).toString());// 订单提交时间
								java.sql.Date accountDate = new java.sql.Date(
										sd.parse(hssfRow.getCell(2).toString()).getTime());// 订单完成时间
								String payNo = hssfRow.getCell(3).toString();// 商户订单号
								passVo.setTransactionTime(new Timestamp(tradTime.getTime()));
								passVo.setAccountDate(accountDate);
								
								BigDecimal amount = new BigDecimal(hssfRow.getCell(6).toString());// 交易金额
								String currency = hssfRow.getCell(7).toString();//币种
								String beneficiaryBankNo = hssfRow.getCell(8).toString();// 客户交易银行账号
								String paymentBankNo = hssfRow.getCell(10).toString();// 平台付款帐号
								String tradStatus = hssfRow.getCell(11).toString();// 交易结果
								String failDetail = hssfRow.getCell(12).toString();// 交易结果描述
								String feeStr = hssfRow.getCell(13).toString();//手续费
								if("0".equals(feeStr)){
									feeStr = feeStr+".00";
								}
//								BigDecimal fee = new BigDecimal(feeStr);// 手续费

								passVo.setChannelId(channelId);// 渠道号
								passVo.setOrderId(payNo);
								passVo.setTransactionAmount(amount);
								passVo.setTransactionCurrency(currency);
								passVo.setPaymentBankNo(paymentBankNo);
								passVo.setBeneficiaryBankNo(beneficiaryBankNo);
								passVo.setFailDetail(failDetail);
								if ("S0000000".equals(tradStatus)) {
									passVo.setTradStatus("1");
								} else {
									passVo.setTradStatus("2");
								}
							}else if("9".equals(channelId) && !"实名支付".equals(tradType) && !"代付".equals(tradType)){//网银支付
								String payNo = hssfRow.getCell(0).toString();// 商户订单号
//								String tradeType = hssfRow.getCell(2).toString();//交易类型,网关支付
								BigDecimal amount = new BigDecimal(hssfRow.getCell(3).toString());//交易金额;
								Timestamp tradTime = new Timestamp(sd.parse(hssfRow.getCell(4).toString()).getTime());//订单交易时间
								String tradStatus = hssfRow.getCell(5).toString();//交易状态，01-处理中，02-成功，03失败，04订单过期，05撤销成功
//								String backAmount = hssfRow.getCell(6).toString();//退款金额
								passVo.setOrderId(payNo);
								passVo.setTransactionAmount(amount);
								passVo.setTransactionTime(tradTime);
								if("02".equals(tradStatus)||"2".equals(tradStatus)){
									passVo.setTradStatus("1");
								}else{
									passVo.setTradStatus("2");
								}
								passVo.setChannelId(channelId);
							}else{
								returnMap.put("responseCode", "FAIL");
								returnMap.put("responseMsg", "选择渠道错误");
								return returnMap;
							}
							passList.add(passVo);
						}
					}
				}
				is.close();
			} else {
				returnMap.put("responseCode", "FAIL");
				returnMap.put("responseMsg", "找不到指定的文件");
				return returnMap;
			}
		} catch (Exception e) {
			returnMap.put("responseCode", "FAIL");
			returnMap.put("responseMsg", "读取文件内容出错");
			log.error("读取文件内容出错",e);
			return returnMap;
		}
		try {
			reconciliationPassDao.save(passList);// 入库
			returnMap.put("responseCode", "SUCCESS");
			returnMap.put("responseMsg", "成功");
		} catch (Exception e) {
			returnMap.put("responseCode", "FAIL");
			returnMap.put("responseMsg", "导入数据库异常");
			log.error("导入数据库异常",e);
		}
		return returnMap;
	}
	
	/**
	 * 宝付获取对账文件
	 * @param checkDate
	 * @param localUrl
	 * @param channelId
	 */
	public Map<String,String> getBaofooRecon(String checkDate,
			String  channelId) {
		Map<String,String> returnMap = new HashMap<String,String>();
		ReconciliationRecordsForm form = new ReconciliationRecordsForm();
		//判断日期是否已上传过对账文件
		ReconciliationRecordsVo vo = reconciliationRecordsService.findByDate(checkDate,channelId);
		form.setReconDate(checkDate);
		form.setChannelId(channelId);
		//获取对账文件并入库时先判断该日期是否已存在
		if(checkDate == null){
			returnMap.put("responseCode", "FAIL");
			returnMap.put("responseMsg", "对账日期为空");
			return returnMap;
		}
		if(vo == null){//未导入对账文件
			ReconciliationPassEvent event = new ReconciliationPassEvent();
			event.setChannel(channelId);
			event.setTradeType("reconciliationPass");
			event.setCheckDate(checkDate);
			publisher.publishEvent(event);//获取对账文件
			if(Constant.SUCCESS.equals(event.getReturnCode())){//下载对账文件成功
				String fileName = event.getFileName();//对账文件名
				returnMap = BFgetReconAndInstall(returnMap,checkDate,channelId,fileName);
				if ("SUCCESS".equals(returnMap.get("responseCode"))) {// 导入对账文件成功
					form.setReconStatus("1");// 导入成功状态
					reconciliationRecordsService.save(form);
					returnMap.put("responseCode", "SUCCESS");
					returnMap.put("responseMsg", "获取对账流水成功");
				}
			}else {
				returnMap.put("responseCode", "FAIL");
				returnMap.put("responseMsg", "获取对账流水失败");
			}
		}else if("1".equals(vo.getReconStatus())){
			returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "已导入过该日期对账文件，无需再次导入");
		}
	return returnMap;
	}

	/**
	 * 宝付读取文件并入库
	 * @param returnMap
	 * @param filePath
	 * @param channelId
	 * @return
	 */
	public Map<String,String> BFgetReconAndInstall(Map<String,String> returnMap,String checkDate,String channelId,String fileName){
		List<ReconciliationPassVo> passList = new ArrayList<ReconciliationPassVo>();
		try {
            String encoding="UTF-8";
            if("Failed".equals(fileName)){
            	returnMap.put("responseCode", "FAIL");
		        returnMap.put("responseMsg", "解析对账文件出错");
		        return returnMap;
            }
            File file=new File(fileName);
            if(file.isFile() && file.exists()){ //判断文件是否存在
            	InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file),encoding);//考虑到编码格式
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = null;
                if(bufferedReader.readLine()!=null){
		        	while((lineTxt = bufferedReader.readLine()) != null){
		        		ReconciliationPassVo passVo = new ReconciliationPassVo();
		        		Date date = new Date();
		        		Timestamp creatDate = new Timestamp(date.getTime());
		                String [] order = lineTxt.split("\\|");//商户号,商户订单号,平台订单号,交易时间(yyyyMMddHHmmss),交易类型(00代付01代收),交易卡号,交易状态,单笔金额,签名信息
		                log.info("order:=={}",JSONObject.toJSON(order));
		                if(order.length<=8) continue;//过滤汇总行
		                String memberId = order[0];//商户号
		                if(!com.guohuai.payadapter.bankutil.StringUtil.isDigital(memberId)) continue;//过滤第一行中文
		                String tradeType = order[2];//交易类型
		                String treadStatus = order[3];//交易子类型，成功00退款01撤销02
		                String outsideOrderNo = order[4];//宝付订单号
		                String payNo = order[5];//商户支付订单号
		                if(StringUtil.isEmpty(payNo) ){
		                	payNo = outsideOrderNo;
		                }
		                log.info("对账流水号 payNo：{}",payNo);
		                String payNoPre = "";//订单号前缀
		          		if ("test".equals(environment) && !StringUtil.isEmpty(payNo)) {//测试环境过滤订单号
		          			if(TradeChannel.baofooDkWithoiding.getValue().equals(channelId) || TradeChannel.baofoopay.getValue().equals(channelId)
		          					|| TradeChannel.baofooGateway.getValue().equals(channelId)){
		              				String seq = seqEnv+"01";
		              				payNoPre = payNo.substring(0, seqEnv.length()+2);
		              				if(!seq.equals(payNoPre)) continue;
		          				}
		          			if(TradeChannel.baofoopayee.getValue().equals(channelId)){
		          				if(payNo.length()<6){
		          					continue;
		          				}
		          				payNoPre = payNo.substring(seqEnv.length(),seqEnv.length()+3);
		          				if(!"SEL".equals(payNoPre)) continue;
		          			}
		          			if(payNo.length()>32) continue;
		          		}
		                String amount = order[8];//交易金额
		                String fee = order[9];//手续费
		                String treadTime = order[11];//订单创建时间
		                if(TradeChannel.baofoopay.getValue().equals(channelId)){
                        	if(!"04311".equals(tradeType)) continue;//不是认证支付的订单
                        }else if(TradeChannel.baofooDkWithoiding.getValue().equals(channelId)){
                        	if(!"10311".equals(tradeType)) continue;//不是代扣的订单
                        }else if(TradeChannel.baofooGateway.getValue().equals(channelId)){
                        	if(!("01311".equals(tradeType)||"01301".equals(tradeType))) continue;//不是网关支付的订单、线下打款
                        }else if(TradeChannel.baofoopayee.getValue().equals(channelId)){
                        	amount = order[9];
                        	fee = order[10];
                        	treadTime = order[14];
                        	//宝付后台代付的，是宝付生成的商户代付订单号 以 BF_M开头， 当拆单时，商户代付订单号相同，但宝付定单号会有多个，此时按宝付定单号对账
                        	if(payNo.startsWith("BF_M")){
                        		payNo = outsideOrderNo;//宝付订单号
                        		log.info("商户后台代付按宝付定单号对账 payNo:{}",payNo);
                        	}
                        }else{
                        	log.info("未知订单类型，商户号{},订单号{},金额{},订单创建时间{},渠道号{}",memberId,payNo,amount,treadTime,channelId);
                        	continue;
                        }
                        //处理金额
		                log.info("解析一行对账文件：商户号{},订单号{},金额{},订单创建时间{},渠道号{}",memberId,payNo,amount,treadTime,channelId);

            			BigDecimal bd = new BigDecimal(amount);
            			BigDecimal bd2 = new BigDecimal(fee);
		                //处理交易时间
		                SimpleDateFormat  sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		        	    Date dates=sd.parse(treadTime);
		        		Timestamp tt = new Timestamp(dates.getTime());
		        		if("00".equals(treadStatus)){
		        			treadStatus = PayEnum.PAY1.getCode();//交易状态：成功
		        		}else{
		        			treadStatus = PayEnum.PAY2.getCode();//交易状态：成功
		        		}
		        		passVo.setCreateTime(creatDate);
		        		passVo.setOrderId(payNo);
		        		passVo.setChannelId(channelId);
		        		passVo.setTradStatus(treadStatus);
		        		passVo.setTransactionAmount(bd);
		                passVo.setTransactionTime(tt);
		                passVo.setMemberId(memberId);
		                passVo.setFee(bd2);
		                passVo.setOutsideOrderNo(outsideOrderNo);
		                passList.add(passVo);
		            }
		            read.close();
                } 
		    }else{
		        returnMap.put("responseCode", "FAIL");
		        returnMap.put("responseMsg", "找不到指定的文件");
		        return returnMap;
		    }
	    } catch (Exception e) {
	        returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "读取文件内容出错");
	        log.error("读取文件内容出错",e);
	        return returnMap;
	    }
		try{
			//分批次入库
			batchAddList(passList);
			returnMap.put("responseCode", "SUCCESS");
	        returnMap.put("responseMsg", "成功");
		}catch(Exception e){
			returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "导入数据库异常");
	        log.error("导入数据库异常",e);
		}
		return returnMap;
	}
	
	/**
	 * 获取易宝对账数据
	 * @param checkDate 对账日期
	 * @param channelId 通道id
	 * @return 对账获取结果
	 */
	public Map<String,String> getYeePayRecon(String checkDate, String  channelId) {
		Map<String,String> returnMap = new HashMap<String,String>();
		ReconciliationRecordsForm form = new ReconciliationRecordsForm();
		//判断日期是否已上传过对账文件
		ReconciliationRecordsVo vo = reconciliationRecordsService.findByDate(checkDate,channelId);
		form.setReconDate(checkDate);
		form.setChannelId(channelId);
		//获取对账文件并入库时先判断该日期是否已存在
		if(checkDate == null){
			returnMap.put("responseCode", "FAIL");
			returnMap.put("responseMsg", "对账日期为空");
			return returnMap;
		}
		if(vo == null){//未导入对账文件
			ReconciliationPassEvent event = new ReconciliationPassEvent();
			event.setChannel(channelId);
			event.setTradeType("reconciliationPass");
			event.setCheckDate(checkDate);
			publisher.publishEvent(event);//获取对账数据
			log.info("获取对账流水，适配器返回信息：{}",JSONObject.toJSONString(event));
			if(Constant.SUCCESS.equals(event.getReturnCode())){//获取对账数据成功
				//合并至荣盛分支需打开此两行代码
//				String data = event.getResultData();//对账数据
//				returnMap = yeePayGetReconAndInstall(returnMap,checkDate,channelId,data);
				if ("SUCCESS".equals(returnMap.get("responseCode"))) {// 导入对账数据成功
					form.setReconStatus("1");// 导入成功状态
					reconciliationRecordsService.save(form);
					returnMap.put("responseCode", "SUCCESS");
					returnMap.put("responseMsg", "获取对账流水成功");
				}
			}else if("TZ9010001".equals(event.getReturnCode())){//易宝获取对账流水无数据
				//判断获取对账的时间，若获取对账时间小于服务器，则认为获取成功，若获取时间为服务器当日九点后，则认为成功
				try {
					SimpleDateFormat  sdf = new SimpleDateFormat("yyyyMMdd");
	        	    Date dates = sdf.parse(checkDate);
	        	    Timestamp checkTime = new Timestamp(dates.getTime());
	        	    String sdate = sdf.format(new Date());
	        	    Date date = sdf.parse(sdate);
	        	    Timestamp nowtime = new Timestamp(date.getTime()-86400000);
	        	    if(nowtime.getTime() == checkTime.getTime()){
	        	    	log.info("获取对账日期为当日，判断是否为9点");
	        	    	Calendar rightNow = Calendar.getInstance();
	        	    	int hour = rightNow.get(Calendar.HOUR_OF_DAY);
	        	    	if(hour >= 9){
	        	    		log.info("时间大于9点，获取对账流水成功");
	        	    		form.setReconStatus("1");// 导入成功状态
	    					reconciliationRecordsService.save(form);
	        	    		returnMap.put("responseCode", "SUCCESS");
	    					returnMap.put("responseMsg", "获取对账流水成功");
	        	    	}else{
	        	    		log.info("时间小于9点，获取对账流水失败");
	        	    		returnMap.put("responseCode", "FAIL");
							returnMap.put("responseMsg", "该日期尚未生成对账数据");
	        	    	}
	        	    }else if(nowtime.getTime() > checkTime.getTime()){
	        	    	log.info("获取对账日期为服务器之前日期，获取对账流水成功");
	        	    	form.setReconStatus("1");// 导入成功状态
						reconciliationRecordsService.save(form);
	        	    	returnMap.put("responseCode", "SUCCESS");
    					returnMap.put("responseMsg", "获取对账流水成功");
	        	    }else{
	        	    	log.info("该日期尚未生成对账数据");
	        	    	returnMap.put("responseCode", "FAIL");
						returnMap.put("responseMsg", "该日期尚未生成对账数据");
	        	    }
				} catch (ParseException e) {
					log.error("日期转换比较异常");
					returnMap.put("responseCode", "FAIL");
					returnMap.put("responseMsg", "获取对账流水失败");
				}
				
			}else{
				returnMap.put("responseCode", "FAIL");
				returnMap.put("responseMsg", "获取对账流水失败");
			}
		}else if("1".equals(vo.getReconStatus())){
			returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "已导入过该日期对账文件，无需再次导入");
		}
		return returnMap;
	}
	
	/**
	 * 解析易宝对账数据
	 * @param returnMap 返回参数
	 * @param checkDate 对账日期
	 * @param channelId 支付通道
	 * @param data 对账数据
	 * @return
	 */
	public Map<String,String> yeePayGetReconAndInstall(Map<String,String> returnMap,String checkDate,String channelId,String data){
		List<ReconciliationPassVo> passList = new ArrayList<ReconciliationPassVo>();
		returnMap.put("channelId",channelId);
//		String data = "2,2.75,4.00,\r\n10014029913,6656537782934905,TZDRc2d459507bf34b64b24bacdce75aa42a,2017-08-22 16:30:00,2.00,2.00,2017-08-23 09:52:47,,WITHDRAW_SUCCESS\r\n10014029913,1462944842885240,TZDRb340b68c41d142959d9e16dcf8d0fb6d,2017-08-24 12:00:00,0.75,2.00,2017-08-24 14:16:07,,WITHDRAW_SUCCESS";  
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data.getBytes(Charset.forName("utf8"))), Charset.forName("utf8")));    
        String line;
        int lineNumber = 1;
        try {
			while ( (line = br.readLine()) != null ) {
			    if(!line.trim().equals("")){
			    	String[] parts = line.split(",");
			    	//总笔数,总金额,总手续费
			    	if(lineNumber==1){
			    		lineNumber = 2;
			    		log.info("总笔数：{},总金额：{},总手续费：{}",parts[0],parts[1],parts[2]);
			    	}else{
			    		log.info("解析一行对账文件：商户号{},订单号{},金额{},订单创建时间{},渠道号{}",parts[0],parts[1],parts[4],parts[3],channelId);
			    		//(提现)商户编号,客户订单号,易宝流水号,请求时间,订单金额,手续费,清算时间,备注,成功失败标识
			    		//(充值)商户编号,客户订单号,易宝流水号,请求时间,订单金额,订单状态,手续费,手续费明细,清算时间,备注
			    		ReconciliationPassVo passVo = new ReconciliationPassVo();
			    		String memberId = parts[0];//商户号
			    		String orderId = parts[1];//订单号
			    		String outsideOrderNo = parts[2];//三方订单号
			    		Timestamp transactionTime = Timestamp.valueOf(parts[3]);//请求时间
			    		BigDecimal transactionAmount = new BigDecimal(parts[4]);//订单金额
			    		BigDecimal fee = BigDecimal.ZERO;
			    		String tradStatus = PayEnum.PAY1.getCode();//交易状态：成功
			    		String failDetail = "支付成功";
			    		String feeStr = parts[5];
			    		String accountDateStr = parts[6];
			    		if("19".equals(channelId)){
			    			if("支付失败".equals(parts[5])){
			    				tradStatus =PayEnum.PAY2.getCode();//交易状态：失败
			    				failDetail = "支付失败";
			    			}
			    			feeStr = parts[6];
			    			accountDateStr = parts[8];
			    		}else{
			    			String failMark = parts[8];
			    			if(!StringUtil.isEmpty(failMark)&&"WITHDRAW_FAIL".equals(failMark)){
			    				tradStatus =PayEnum.PAY2.getCode();//交易状态：失败
			    				failDetail = "支付失败";
			    			}
			    			
			    		}
			    		fee = new BigDecimal(feeStr);
			    		Date now = new Date();
			    	    Timestamp time = new Timestamp(now.getTime());//当前时间
			    		passVo.setCreateTime(time);
			    		passVo.setUpdateTime(time);
			    		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			    		java.util.Date date = sdf.parse(accountDateStr);
						passVo.setAccountDate(new java.sql.Date(date.getTime()));
			    		passVo.setOrderId(orderId);
			    		passVo.setChannelId(channelId);
			    		passVo.setTradStatus(tradStatus);
			    		passVo.setTransactionAmount(transactionAmount);
			            passVo.setTransactionTime(transactionTime);
			            passVo.setMemberId(memberId);
			            passVo.setFee(fee);
			            passVo.setOutsideOrderNo(outsideOrderNo);
			            passVo.setFailDetail(failDetail);
			            passList.add(passVo);
			    	}
			    }
			}
		} catch (IOException | ParseException e) {
			returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "读取对账数据内容出错");
	        log.error("读取对账数据内容出错",e);
	        return returnMap;
		}   
		try{
			//分批次入库
			batchAddList(passList);
			returnMap.put("responseCode", "SUCCESS");
	        returnMap.put("responseMsg", "成功");
		}catch(Exception e){
			returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "导入数据库异常");
	        log.error("导入数据库异常",e);
		}
		return returnMap;
	}
	
	/**
	 * 导入对账数据分批次入库
	 * @param passList
	 */
	public void batchAddList(List<ReconciliationPassVo> passList){
		//分批处理  
		if(null!=passList && passList.size()>0){
			int pointsDataLimit = 1000;//限制条数  
			Integer size = passList.size();  
			//判断是否有必要分批  
			if(pointsDataLimit<size){
				int part = size/pointsDataLimit;//分批数 
				int logPart = part+1;
				log.info("导入对账数据共有 ： "+size+"条，！"+" 分为 ："+logPart+"批");
				
				for (int i = 0; i < part; i++) {  
					//1000条
					List<ReconciliationPassVo> listPage = passList.subList(0, pointsDataLimit);
					reconciliationPassDao.save(listPage);//分批次入库
					//剔除
					passList.subList(0, pointsDataLimit).clear();
				}
				if(!passList.isEmpty()){
					reconciliationPassDao.save(passList);//剩余数据入库
				}
			}else{//无需分批次，直接入库
				reconciliationPassDao.save(passList);//数据入库
			}
		}else{
			log.info("无对账数据!!!");
		}  
	}
	
	/**
	 * 对账忽略
	 * 
	 * @param oids
	 */
	public void ignore(String[] oids) {
		log.info("忽略对账oids,{}", JSONObject.toJSONString(oids));
		try {
			if (oids.length > 0) {
				for (int i = 0; i < oids.length; i++) {
					reconciliationPassDao.updateStatus(oids[i]);
				}
			}
		} catch (Exception e) {
			log.error("根据oid忽略对账异常:" + e);
		}
	}
	
	/**
	 * 先锋支付导入对账文件
	 * @param checkDate
	 * @param localUrl
	 * @param channelId
	 */
	public Map<String,String> uploadUcfRecon(String checkDate,String channelId,
			MultipartFile upFile) {
		Map<String,String> returnMap = new HashMap<String,String>();
		//读取txt文件并入库
		returnMap = ucfGetReconAndInstall(returnMap,channelId,upFile);
		//记录导入记录
		ReconciliationRecordsForm form = new ReconciliationRecordsForm();
		form.setReconDate(checkDate);
		form.setChannelId(channelId);//新增字段渠道
		if("SUCCESS".equals(returnMap.get("responseCode"))){//导入成功
			form.setReconStatus("1");//导入成功状态
			reconciliationRecordsService.save(form);
		}
		return returnMap;
	}
	
	/**
	 * 先锋支付读取文件并入库
	 * @param returnMap
	 * @param filePath
	 * @param channelId
	 * @return
	 */
	public Map<String,String> ucfGetReconAndInstall(Map<String,String> returnMap,String channelId,MultipartFile file){
		List<ReconciliationPassVo> passList = new ArrayList<ReconciliationPassVo>();
		try {
            String encoding="UTF-8";
            InputStream in = file.getInputStream();
            if(!file.isEmpty()){ //判断文件是否为空
            	log.info("read recon txt start====================文件大小："+file.getSize());
            	InputStreamReader read = new InputStreamReader(in,encoding);//考虑到编码格式
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = null;
                while((lineTxt = bufferedReader.readLine()) != null){
            		ReconciliationPassVo passVo = new ReconciliationPassVo();
            		Date date = new Date();
            		Timestamp creatDate = new Timestamp(date.getTime());
            		passVo.setCreateTime(creatDate);
                	passVo.setChannelId(channelId);
                    String [] order = lineTxt.split("\\|");//商户号|商户订单号|支付订单号|交易类型|金额|币种|状态|交易完成时间|保留字|50197d42a758cb5b9e6c9f23fa424904
                    passVo.setProductId(order[0]);
                    passVo.setMemberId(order[0]);
                    String orderId = order[1];
                    passVo.setOutsideOrderNo(order[2]);
                    passVo.setOrderId(orderId);
                    String tradeType = (order[3]);//新增字段,交易类型（直接格根据渠道判断，不需要）
                    if(tradeType != null&&"16".equals(tradeType)&&"DK".equals(orderId.substring(0, 2))){
                    	passVo.setChannelId("18");//代扣
                    }
                    String amount = order[4];
                    //处理金额
        			BigDecimal bd = new BigDecimal(amount);
        			BigDecimal b2 = new BigDecimal(100);
        			BigDecimal b3 = bd.divide(b2).setScale(4);
                    passVo.setTransactionAmount(b3);
                    passVo.setTransactionCurrency(order[5]);
                    String treadStatus = order[6];
                    //处理交易状态（S成功F失败）
                    if("S".equals(treadStatus)){
                    	treadStatus = PayEnum.PAY1.getCode();
                    }else {
                    	treadStatus = PayEnum.PAY2.getCode();
                    }
                    passVo.setTradStatus(treadStatus);
                    String treadTime = order[7];
                    //处理交易时间
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");//2016-01-04 01:04:08
                    Timestamp tt = new Timestamp(format.parse(treadTime).getTime());
                    passVo.setTransactionTime(tt);
                    passVo.setFee(BigDecimal.ZERO);
                    passList.add(passVo);
                }
                read.close();
                log.info("read recon txt end====================");
		    }else{
		    	log.info("对账文件为空，无需解析数据，标记为成功");
		    	returnMap.put("responseCode", "SUCCESS");
		        returnMap.put("responseMsg", "成功");
		        return returnMap;
		    }
	    } catch (Exception e) {
	        returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "读取文件内容出错");
	        e.printStackTrace();
	        return returnMap;
	    }
		try{
			log.info("save recon start ============共"+passList.size()+"条");
			//分批次入库
			batchAddList(passList);
			returnMap.put("responseCode", "SUCCESS");
	        returnMap.put("responseMsg", "成功");
	        log.info("save recon end ============");
		}catch(Exception e){
			returnMap.put("responseCode", "FAIL");
	        returnMap.put("responseMsg", "导入数据库异常");
	        e.printStackTrace();
		}
		return returnMap;
	}

}
