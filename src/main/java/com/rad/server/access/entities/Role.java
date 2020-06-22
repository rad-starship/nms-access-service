package com.rad.server.access.entities;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;

@Entity
public class Role 
{
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Id
	private long	id;
	private String	name;


	@ElementCollection
    private List<String> permissions;

	public Role()
	{
		
	}
	


	public Role(String name) {
		this.name = name;
		permissions = new LinkedList<>();
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
		return "Role [id=" + this.id + ", name=" + this.name +"]";
	}

	public void setName(String name) {
		this.name = name;
	}


	public List<String> getPermissions() {
		return permissions;
	}

	public void addPermission(List<String> permissions){
		this.permissions = permissions;
	}
}