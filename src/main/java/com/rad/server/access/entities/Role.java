package com.rad.server.access.entities;

import javax.persistence.*;

@Entity
public class Role 
{
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Id
	private long	id;
	private String	name;

	public Role()
	{
		
	}
	
	public Role(String name)
	{
		this.name = name;
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

	@Override
	public String toString()
	{
		return "User [id=" + this.id + ", name=" + this.name + "]";
	}
}