package com.example.attendance.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.attendance.constants.JobPosition;
import com.example.attendance.constants.RtnCode;
import com.example.attendance.entity.AuthCode;
import com.example.attendance.entity.Employee;
import com.example.attendance.entity.ResignApplication;
import com.example.attendance.repository.AuthCodeDao;
import com.example.attendance.repository.DepartmentsDao;
import com.example.attendance.repository.EmployeeDao;
import com.example.attendance.repository.ResignApplicationDao;
import com.example.attendance.service.ifs.EmployeeService;
import com.example.attendance.vo.BasicRes;
import com.example.attendance.vo.EmployeeCreateReq;
import com.example.attendance.vo.EmployeeRes;

import net.bytebuddy.utility.RandomString;

@Service
public class EmployeeServiceImpl implements EmployeeService {

	@Autowired
	private EmployeeDao dao;

	@Autowired
	private DepartmentsDao departmentsDao;

	@Autowired
	private AuthCodeDao authCodeDao;
	
	@Autowired
	private ResignApplicationDao resignApplicationDao;

	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Value("${authcode.expired.time}")
	private int authCodeExpiredTime;

	// impotr slf4j
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public BasicRes create(EmployeeCreateReq req) {
		if (!StringUtils.hasText(req.getId()) || !StringUtils.hasText(req.getDepartment())
				|| !StringUtils.hasText(req.getName()) || !StringUtils.hasText(req.getPwd())
				|| !StringUtils.hasText(req.getEmail()) || !StringUtils.hasText(req.getJobPosition())
				|| req.getBirthDate() == null || req.getArrivalDate() == null) {
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		if (dao.existsById(req.getId())) {
			return new BasicRes(RtnCode.ID_HAS_EXISTED);
		}
		// check department name
		if (!departmentsDao.existsByName(req.getDepartment())) {
			return new BasicRes(RtnCode.DEPARTMENT_NOT_FOUND);
		}
		req.setPwd(encoder.encode(req.getPwd()));
		// dao.save((Employee)req);
		// dao.save(req);
		try {
			dao.save((Employee) req);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new BasicRes(RtnCode.EMPLOYEE_CREATE_ERROR);
		}
		return new BasicRes(RtnCode.SUCCESSFUL);

	}

	@Override
	public BasicRes login(String id, String pwd, HttpSession session) {
		if (!StringUtils.hasText(id) || !StringUtils.hasText(pwd)) {
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		Optional<Employee> op = dao.findById(id);
		if (op.isEmpty()) {
			return new BasicRes(RtnCode.ID_NOT_FOUND);
		}
		Employee employee = op.get();
		if (!encoder.matches(pwd, employee.getPwd())) {
			return new BasicRes(RtnCode.PASSWORD_ERROR);
		}
		if (!employee.isActive()) {
			return new BasicRes(RtnCode.ACCOUNT_DEACTIVATE);
		}
		session.setAttribute(id, employee.getDepartment());
		// session存活時間 預設 1800 單位 秒
		session.setMaxInactiveInterval(3600);
		logger.info("loging successful!!");
		return new BasicRes(RtnCode.SUCCESSFUL);
	}

	@Override
	public BasicRes changePassword(String id, String oldPwd, String newPwd) {
		if (!StringUtils.hasText(id) || !StringUtils.hasText(oldPwd) || !StringUtils.hasText(newPwd)) {
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		if (oldPwd.equals(newPwd)) {
			return new BasicRes(RtnCode.OLD_PASSWORD_AND_NEW_PASSWORD_ARE_IDENTICAL);
		}
		// 不用判斷是否為空，因為此方法必須是login後才能使用，在login方法那已有判斷
		Employee employee = dao.findById(id).get();
		if (!encoder.matches(oldPwd, employee.getPwd())) {
			return new BasicRes(RtnCode.PASSWORD_ERROR);
		}
		employee.setPwd(encoder.encode(newPwd));
		try {
			dao.save(employee);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new BasicRes(RtnCode.CHANGE_PASSWORD_ERROR);
		}
		return new BasicRes(RtnCode.SUCCESSFUL);
	}

	@Override
	public BasicRes forgotPassword(String id, String email) {
		if (!StringUtils.hasText(id) && !StringUtils.hasText(email)) {
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		Employee employee = null;
		if (StringUtils.hasText(id)) {
			Optional<Employee> op = dao.findById(id);   
			if (op.isEmpty()) {
				return new BasicRes(RtnCode.ID_NOT_FOUND);
			}
			employee = op.get();
		} else {
			employee = dao.findByEmail(email);
			if (employee == null) {
				return new BasicRes(RtnCode.ID_NOT_FOUND);
			}
		}
		String randomPwd = RandomString.make(12);
		employee.setPwd(encoder.encode(randomPwd));
		// 產生驗證碼,有效時間30分鐘
		String authCode = RandomString.make(6);
		LocalDateTime now = LocalDateTime.now();
		try {
			dao.save(employee);
			authCodeDao.save(new AuthCode(employee.getId(), authCode, now.plusMinutes(authCodeExpiredTime)));
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new BasicRes(RtnCode.FORGOT_PASSWORD_ERROR);
		}

		// 界接 email 並寄送 auth_code
		return new BasicRes(RtnCode.SUCCESSFUL);
	}

	@Override
	public BasicRes changePasswordByAuthCode(String id, String authCode, String newPwd) {
		if (!StringUtils.hasText(id) || !StringUtils.hasText(authCode) || !StringUtils.hasText(newPwd)) {
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		// check auth_code
		Optional<AuthCode> op = authCodeDao.findById(id);
		if (op.isEmpty()) {
			return new BasicRes(RtnCode.ID_NOT_FOUND);
		}
		AuthCode authCodeEntity = op.get();
		if (!authCodeEntity.getAuthCode().equals(authCode)) {
			return new BasicRes(RtnCode.AUTH_CODE_NOT_MATCHED);
		}
		LocalDateTime now = LocalDateTime.now();
		if (now.isAfter(authCodeEntity.getAuthDatetime())) {
			return new BasicRes(RtnCode.AUTH_CODE_EXPIRED);
		}
		Employee employee = dao.findById(id).get();
		employee.setPwd(encoder.encode(newPwd));
		try {
			dao.save(employee);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new BasicRes(RtnCode.CHANGE_PASSWORD_ERROR);
		}
		return new BasicRes(RtnCode.SUCCESSFUL);
	}

	@Deprecated //此方法棄用(方法名稱標記刪除線),由updateActive取代
	@Override
	public BasicRes activate(String executorId, String employeeId) {
		if (!StringUtils.hasText(executorId) || !StringUtils.hasText(employeeId) || executorId.equals(employeeId)) {
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		// 不用判斷是否為空，因為此方法必須是login後才能使用，在login方法那已有判斷
		Employee executor = dao.findById(executorId).get();
		if (!executor.getDepartment().equalsIgnoreCase("admin") && !executor.getDepartment().equalsIgnoreCase("HR")) {
			return new BasicRes(RtnCode.UNAUTHORIZATED);
		}
		if (dao.updateActivate(employeeId, true) != 1) {
			return new BasicRes(RtnCode.UPDATE_FAILED);
		}
		return new BasicRes(RtnCode.SUCCESSFUL);
	}

	@Deprecated //此方法棄用(方法名稱標記刪除線),由updateActive取代
	@Override
	public BasicRes deactivate(String executorId, String employeeId) {
		if (!StringUtils.hasText(executorId) || !StringUtils.hasText(employeeId) || executorId.equals(employeeId)) {
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		// 不用判斷是否為空，因為此方法必須是login後才能使用，在login方法那已有判斷
		Employee executor = dao.findById(executorId).get();
		if (!executor.getDepartment().equalsIgnoreCase("admin") && !executor.getDepartment().equalsIgnoreCase("HR")) {
			return new BasicRes(RtnCode.UNAUTHORIZATED);
		}
		if (dao.updateActivate(employeeId, false) != 1) {
			return new BasicRes(RtnCode.UPDATE_FAILED);
		}
		return new BasicRes(RtnCode.SUCCESSFUL);
	}

	@Override
	public BasicRes updateActivate(String executorId, String employeeId, boolean isActive) {
		// 不用判斷 executorId 是否為空，因為此方法必須是login後才能使用，在login方法那已有判斷
		if (!StringUtils.hasText(employeeId) || executorId.equals(employeeId)) {
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		// 不用判斷是否為空，因為此方法必須是login後才能使用，在login方法那已有判斷
		Employee executor = dao.findById(executorId).get();
		if (!executor.getDepartment().equalsIgnoreCase("admin") && !executor.getDepartment().equalsIgnoreCase("HR")) {
			return new BasicRes(RtnCode.UNAUTHORIZATED);
		}
		try {
			int res = dao.updateActivate(employeeId, isActive);
			if (res != 1) {
				return new BasicRes(RtnCode.UPDATE_FAILED);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new BasicRes(RtnCode.UPDATE_ERROR);
		}
		return new BasicRes(RtnCode.SUCCESSFUL);
	}

	@Override
	public BasicRes updateResign(String executorId, String employeeId) {
		// 不用判斷 executorId 是否為空，因為此方法必須是login後才能使用，在login方法那已有判斷
		if (!StringUtils.hasText(employeeId) || executorId.equals(employeeId)) {
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		// 不用判斷是否為空，因為此方法必須是login後才能使用，在login方法那已有判斷
		Employee executor = dao.findById(executorId).get();
		if (executor.getDepartment().equalsIgnoreCase("HR")) {
			return new BasicRes(RtnCode.UNAUTHORIZATED);
		}
		ResignApplication application = resignApplicationDao.findByEmployeeId(employeeId);
		Employee employee = dao.findById(employeeId).get();
		employee.setResignationDate(application.getResignationDate());
		employee.setQuitReason(application.getQuitReason());
		try {
			dao.save(employee);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		return new BasicRes(RtnCode.SUCCESSFUL);
	}

	@Override
	public BasicRes resignApplication(String employeeId) {
		// 不用判斷 employeeId 是否為空，因為此方法必須是login後才能使用，在login方法那已有判斷
		Employee employee = dao.findById(employeeId).get();
		try {
			resignApplicationDao.save(new ResignApplication(employeeId, employee.getDepartment(), LocalDate.now().plusMonths(1),"學長欺負我!!"));
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		return new BasicRes(RtnCode.SUCCESSFUL);
	}

	@Override
	public BasicRes updateInfo(String executorId, Employee employee) {
		// 不用判斷 executorId 是否為空，因為此方法必須是login後才能使用，在login方法那已有判斷
		if (!StringUtils.hasText(employee.getId()) || executorId.equals(employee.getId())) {
			return new BasicRes(RtnCode.PARAM_ERROR);
		}
		try {
			int res = dao.updateInfo(employee.getId(), employee.getDepartment(), employee.getName(), employee.getEmail(), 
					employee.getJobPosition(), employee.getBirthDate(), employee.getArrivalDate(),
					employee.getAnnualLeave(), employee.getSickLeave());
			if (res != 1) {
				return new BasicRes(RtnCode.UPDATE_FAILED);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new BasicRes(RtnCode.UPDATE_ERROR);
		}
		return new BasicRes(RtnCode.SUCCESSFUL);
	}

	@Override
	public EmployeeRes findByEmployeeId(String employeeId) {
		// 不用判斷 employeeId 是否為空，因為此方法必須是login後才能使用，在login方法那已有判斷
		Employee employee = dao.findById(employeeId).get();
		return new EmployeeRes(RtnCode.SUCCESSFUL, employee);
	}

	@Override
	public EmployeeRes findStaffInfo(String callerId, String targetId) {
		// 不用判斷 callerId 是否為空，因為此方法必須是login後才能使用，在login方法那已有判斷
		if(!StringUtils.hasText(targetId)) {
			return new EmployeeRes(RtnCode.PARAM_ERROR, null);
		}
		Optional<Employee> op = dao.findById(targetId);
		if(op.isEmpty()) {
			return new EmployeeRes(RtnCode.ID_NOT_FOUND, null);
		}
		Employee targetInfo = op.get();
		Employee callerInfo = dao.findById(callerId).get();
		String callerDepartment = callerInfo.getDepartment();
		//1.同部門 且caller是單位主管 2.caller是HR部門
		if ((callerDepartment.equals(targetInfo.getDepartment()) 
				&& JobPosition.hasReviewPermission(callerInfo.getJobPosition()))
				|| callerDepartment.equalsIgnoreCase("HR")) {
			return new EmployeeRes(RtnCode.SUCCESSFUL, targetInfo);
		}
		return new EmployeeRes(RtnCode.PERMISSION_DENIED, null);
	}

}
