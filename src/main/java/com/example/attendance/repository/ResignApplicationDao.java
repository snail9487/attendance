package com.example.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.attendance.entity.ResignApplication;

public interface ResignApplicationDao extends JpaRepository <ResignApplication, Integer>{

	public ResignApplication findByEmployeeId(String employeeId);
}
