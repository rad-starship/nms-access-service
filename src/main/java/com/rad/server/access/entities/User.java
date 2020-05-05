package com.rad.server.access.entities;

import javax.jws.Oneway;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	@ElementCollection
	private List<Long> tenantsID;


	public User()
	{
		
	}

	public long getRoleID() {
		return roleID;
	}

	public List<Long> getTenantID() {
		return tenantsID;
	}

	/**
	 * @param firstName
	 * @param lastName
	 * @param email
	 * @param userName
	 */
	public User(String firstName,String lastName, String email,String userName,long roleID,Long[] tenants)
	{
		tenantsID=new ArrayList<>();
		this.firstName = firstName;
		this.lastName=lastName;
		this.email = email;
		this.userName=userName;
		this.roleID=roleID;
		tenantsID.addAll(Arrays.asList(tenants));
	}

	public User(String firstName,String lastName, String email,String userName,long roleID,ArrayList<Long> tenants)
	{
		this.firstName = firstName;
		this.lastName=lastName;
		this.email = email;
		this.userName=userName;
		this.roleID=roleID;
		tenantsID=tenants;
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

	public void setRoleID(long roleID) {
		this.roleID = roleID;
	}

	public void setTenantsID(List<Long> tenantsID) {
		this.tenantsID = tenantsID;
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