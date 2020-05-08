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

	private int tokenMinutes;
	private int tokenHours;
	private int tokenDays;
	private int accessTokenTimeout;

	public Tenant()
	{
		
	}
	
	public Tenant(String name,int tokenMinutes,int tokenHours,int tokenDays,int accessTokenTimeout)
	{
		this.name = name;
		this.tokenMinutes=tokenMinutes;
		this.tokenHours=tokenHours;
		this.tokenDays=tokenDays;
		this.accessTokenTimeout=accessTokenTimeout;
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

	public void setName(String name) {
		this.name = name;
	}

	public int getTokenMinutes() {
		return tokenMinutes;
	}

	public void setTokenMinutes(int tokenMinutes) {
		this.tokenMinutes = tokenMinutes;
	}

	public int getTokenHours() {
		return tokenHours;
	}

	public void setTokenHours(int tokenHours) {
		this.tokenHours = tokenHours;
	}

	public int getTokenDays() {
		return tokenDays;
	}

	public void setTokenDays(int tokenDays) {
		this.tokenDays = tokenDays;
	}

	public int getAccessTokenTimeout() {
		return accessTokenTimeout;
	}

	public void setAccessTokenTimeout(int accessTokenTimeout) {
		this.accessTokenTimeout = accessTokenTimeout;
	}

	@Override
	public String toString()
	{
		return "User [id=" + this.id + ", name=" + this.name + "]";
	}
}