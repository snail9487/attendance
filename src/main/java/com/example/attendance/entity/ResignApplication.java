package com.example.attendance.entity;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "resign_application")
public class ResignApplication {

	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	@Column(name = "id")
	private int id;
	
	@Column(name = "employee_id")
	private String employeeId;
	
	@Column(name = "dapartment")
	private String dapartment;
	
	@Column(name = "resignation_date")
	private LocalDate resignationDate;
	
	@Column(name = "quit_reason")
	private String quitReason;
	
	@Column(name = "director_review")
	private boolean directorReview;
	
	@Column(name = "hr_review")
	private boolean hrReview;

	public ResignApplication() {
		super();
	}

	public ResignApplication(String employeeId, String dapartment, LocalDate resignationDate, String quitReason) {
		super();
		this.employeeId = employeeId;
		this.dapartment = dapartment;
		this.resignationDate = resignationDate;
		this.quitReason = quitReason;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeeId(String employeeId) {
		this.employeeId = employeeId;
	}

	public String getDapartment() {
		return dapartment;
	}

	public void setDapartment(String dapartment) {
		this.dapartment = dapartment;
	}

	public LocalDate getResignationDate() {
		return resignationDate;
	}

	public void setResignationDate(LocalDate resignationDate) {
		this.resignationDate = resignationDate;
	}

	public String getQuitReason() {
		return quitReason;
	}

	public void setQuitReason(String quitReason) {
		this.quitReason = quitReason;
	}

	public boolean isDirectorReview() {
		return directorReview;
	}

	public void setDirectorReview(boolean directorReview) {
		this.directorReview = directorReview;
	}

	public boolean isHrReview() {
		return hrReview;
	}

	public void setHrReview(boolean hrReview) {
		this.hrReview = hrReview;
	}
	
	
	
	
}
