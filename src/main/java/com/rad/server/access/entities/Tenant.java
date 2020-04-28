/**
 * 
 */
package com.rad.server.access.entities;

import org.hibernate.validator.constraints.UniqueElements;

import javax.persistence.*;

/**
 * @author raz_o
 *
 */
@Entity
public class Tenant
{
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Id
	private long	id;
	@Column(unique = true)
	private String	name;

	public Tenant()
	{
		
	}
	
	public Tenant(String name)
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