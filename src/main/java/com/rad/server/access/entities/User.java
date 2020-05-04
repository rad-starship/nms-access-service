package com.rad.server.access.entities;

import javax.persistence.*;

@Entity
public class User
{
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Id
	private long	id;
	private String	firstName;
	private String	lastName;
	private String	email;
	@Column(unique=true)
	private String	userName;
	private long roleID;
	private long tenantID;


	public User()
	{
		
	}

	public long getRoleID() {
		return roleID;
	}

	public long getTenantID() {
		return tenantID;
	}

	/**
	 * @param firstName
	 * @param lastName
	 * @param email
	 * @param userName
	 */
	public User(String firstName,String lastName, String email,String userName,long roleID,long tenantID)
	{
		this.firstName = firstName;
		this.lastName=lastName;
		this.email = email;
		this.userName=userName;
		this.roleID=roleID;
		this.tenantID=tenantID;
	}

	public User(String firstName,String lastName, String email,String userName)
	{
		this.firstName = firstName;
		this.lastName=lastName;
		this.email = email;
		this.userName=userName;
	}

	public User(User user){
		this.firstName=user.firstName;
		this.lastName=user.lastName;
		this.email=user.email;
	}

	public long getId()
	{
		return this.id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getFirstName()
	{
		return this.firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	public String getEmail()
	{
		return this.email;
	}


	@Override
	public String toString()
	{
		return "User [id=" + this.id + ", name=" + this.firstName+" "+this.lastName + ", email=" + this.email + "]";
	}
}