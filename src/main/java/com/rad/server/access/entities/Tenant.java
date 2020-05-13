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

	private int SSOSessionIdle;
	private int SSOSessionMax;
	private int offlineSessionIdle;
	private int accessTokenLifespan;

	public Tenant()
	{
		
	}
	
	public Tenant(String name,int tokenMinutes,int tokenHours,int tokenDays,int accessTokenTimeout)
	{
		this.name = name;
		this.SSOSessionIdle=tokenMinutes;
		this.SSOSessionMax=tokenHours;
		this.offlineSessionIdle=tokenDays;
		this.accessTokenLifespan=accessTokenTimeout;
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

	public int getSSOSessionIdle() {
		return SSOSessionIdle;
	}

	public void setSSOSessionIdle(int SSOSessionIdle) {
		this.SSOSessionIdle = SSOSessionIdle;
	}

	public int getSSOSessionMax() {
		return SSOSessionMax;
	}

	public void setSSOSessionMax(int SSOSessionMax) {
		this.SSOSessionMax = SSOSessionMax;
	}

	public int getOfflineSessionIdle() {
		return offlineSessionIdle;
	}

	public void setOfflineSessionIdle(int offlineSessionIdle) {
		this.offlineSessionIdle = offlineSessionIdle;
	}

	public int getAccessTokenLifespan() {
		return accessTokenLifespan;
	}

	public void setAccessTokenLifespan(int accessTokenLifespan) {
		this.accessTokenLifespan = accessTokenLifespan;
	}

	@Override
	public String toString()
	{
		return "User [id=" + this.id + ", name=" + this.name + "]";
	}
}