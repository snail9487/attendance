package com.example.attendance;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.Assert;

import com.example.attendance.entity.Employee;
import com.example.attendance.repository.EmployeeDao;

@SpringBootTest
public class EmployeeTests {

	@Autowired EmployeeDao employeeDao;
	
	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	
	@Test 
	public void createAdminTest() {
		Employee employee = employeeDao.save(new Employee("Admin", "ADMIN", "Admin", 
				encoder.encode("admin123"), "admin@gmail.com", true, "99", LocalDate.now(), LocalDate.now()));
		Assert.isTrue(employee != null, "Create employee error!!");
		
	}
}
