/**
 * 
 */
package com.rad.server.access.entities;

import org.hibernate.validator.constraints.UniqueElements;

import javax.persistence.*;
import java.util.List;

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
	@ElementCollection(fetch = FetchType.EAGER)
	private List<String> continents;

	public Tenant()
	{
		
	}

	public Tenant(String name)
	{
		this.name = name;
	}
	
	public Tenant(String name,List<String> continents)
	{
		this.name = name;
		this.continents=continents;
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

	public List<String> getContinents() {
		return continents;
	}

	public void setContinents(List<String> continents) {
		this.continents = continents;
	}

	public void setName(String name) {
		this.name = name;
	}


	@Override
	public String toString()
	{
		return "User [id=" + this.id + ", name=" + this.name + "]";
	}
}