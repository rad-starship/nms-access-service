package com.rad.server.access.entities;

import javax.persistence.*;

@Entity
public class User
{
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Id
	private long	id;
	private String	name;
	private String	email;

	public User()
	{
		
	}
	
	/**
	 * @param id
	 * @param name
	 * @param email
	 */
	public User(String name, String email)
	{
		this.name = name;
		this.email = email;
	}

	public long getId()
	{
		return this.id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return this.name;
	}

	public String getEmail()
	{
		return this.email;
	}

	@Override
	public String toString()
	{
		return "User [id=" + this.id + ", name=" + this.name + ", email=" + this.email + "]";
	}
}