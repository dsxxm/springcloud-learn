package com.macro.cloud.feign.dto;

/**
 * User
 */
public class User {

	private long id;
	private String name;
	private String passwd;

	public User() {
	}

	public User(long id, String name, String passwd) {
		this.id = id;
		this.name = name;
		this.passwd = passwd;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPasswd() {
		return passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
