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
	private String	userName;


	public User()
	{
		
	}
	
	/**
	 * @param firstName
	 * @param lastName
	 * @param email
	 * @param userName
	 */
	public User(String firstName,String lastName, String email,String userName)
	{
		this.firstName = firstName;
		this.lastName=lastName;
		this.email = email;
		this.userName=userName;
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