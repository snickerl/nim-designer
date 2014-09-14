package com.poweruniverse.nim.designer.utils;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Order;

/**
 * Native SQL for Criteria {@link Order}.
 * <p>
 * <b>Caution</b>: take extra caution with user input in raw sql, may cause SQL injection!
 * <p>
 * Usage:
 * 
 * <pre>
 * criteria.addOrder(NativeSQLOrder.asc(&quot;whatever SQL using {alias}&quot;));
 * criteria.addOrder(NativeSQLOrder.desc(&quot;whatever SQL using {alias}&quot;));
 * criteria.addOrder(NativeSQLOrder.raw(&quot;{alias} asc nulls first&quot;));
 * </pre>
 * 
 * @author Sami Dalouche
 * @author medon
 * 
 * @see http://opensource.atlassian.com/projects/hibernate/browse/HHH-2381
 * 
 */
public class NativeSQLOrder extends Order {
	private static final long serialVersionUID = 1L;
	private final static String PROPERTY_NAME = "uselessAnyways";
	private final Boolean ascending;
	private final String sql;

	/**
	 * Constructor for ascending or descending order.
	 * 
	 * @param sql
	 * @param ascending
	 *            true - ascending, false descending.
	 */
	public NativeSQLOrder(String sql, boolean ascending) {
		super(PROPERTY_NAME, ascending);
		this.sql = sql;
		this.ascending = ascending;
	}

	/**
	 * Constructor for really raw SQL order - no asc or desc added.
	 * 
	 * @param sql
	 * @param ascending
	 */
	public NativeSQLOrder(String sql) {
		super(PROPERTY_NAME, true); // true for ascending just to satisfy the super constructor
		this.sql = sql;
		this.ascending = null;
	}

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		StringBuilder fragment = new StringBuilder(this.sql.length() + 10);

		this.applyAliases(fragment, criteria, criteriaQuery);
		if (this.ascending != null) {
			fragment.append(this.ascending ? " asc" : " desc");
		}

		return fragment.toString();
	}

	@Override
	public String toString() {
		StringBuilder fragment = new StringBuilder(this.sql.length() + 10);

		fragment.append(this.sql);
		if (this.ascending != null) {
			fragment.append(this.ascending ? " asc" : " desc");
		}

		return fragment.toString();
	}

	/**
	 * Substitute aliases in {@link #sql}.
	 * 
	 * @param res
	 * @param criteria
	 * @param criteriaQuery
	 */
	private void applyAliases(StringBuilder res, Criteria criteria, CriteriaQuery criteriaQuery) {
		int i = 0;
		int cnt = this.sql.length();
		while (i < cnt) {
			int l = this.sql.indexOf('{', i);
			if (l == -1) {
				break;
			}

			String before = this.sql.substring(i, l);
			res.append(before);

			int r = this.sql.indexOf('}', l);
			String alias = this.sql.substring(l + 1, r);
			if (alias==null || alias.length()==0  || "alias".equals(alias)) { // root alias
				res.append(criteriaQuery.getSQLAlias(criteria));
			} else {
				String[] columns = criteriaQuery.getColumnsUsingProjection(criteria, alias);
				if (columns.length != 1)
					throw new HibernateException("NativeSQLOrder may only be used with single-column properties: " + alias);
				res.append(columns[0]);
			}
			i = r + 1;
		}
		String after = this.sql.substring(i, cnt);
		res.append(after);
	}

	/**
	 * @param sql
	 * @return new asceding native order.
	 */
	public static Order asc(String sql) {
		return new NativeSQLOrder(sql, true);
	}

	/**
	 * @param sql
	 * @return descending native order.
	 */
	public static Order desc(String sql) {
		return new NativeSQLOrder(sql, false);
	}

	/**
	 * @param sql
	 * @return really raw SQL order - no asc or desc added.
	 */
	public static Order raw(String sql) {
		return new NativeSQLOrder(sql);
	}

}
