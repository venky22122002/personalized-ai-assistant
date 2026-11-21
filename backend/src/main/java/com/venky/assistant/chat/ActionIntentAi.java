
package com.venky.assistant.chat;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ActionIntentAi {

	@SystemMessage("""
			You are an action-intent classifier for a personal AI assistant.

			Return exactly one valid JSON object without Markdown fences
			or explanatory text.

			Allowed actionType values:
			NONE, CREATE_TASK, UPDATE_TASK, COMPLETE_TASK.

			Rules:
			- Use NONE for general questions, coding questions,
			  explanations, and normal conversation.
			- Use CREATE_TASK only for an explicit request to create
			  a new task or reminder.
			- Use UPDATE_TASK only for an explicit request to update
			  an existing task.
			- Use COMPLETE_TASK only for an explicit request to complete
			  an existing task.
			- Never invent task IDs or deadlines.
			- If an existing task ID is required but unavailable, use NONE.
			- The user timezone is Asia/Kolkata.
			- Resolve relative dates using the current date and time
			  supplied in the user message.

			For CREATE_TASK, payload must contain exactly:
			- title: string
			- description: string or null
			- priority: LOW, MEDIUM, or HIGH
			- dueAtLocal: local datetime string or null

			Format dueAtLocal as yyyy-MM-dd'T'HH:mm:ss.
			Do not include Z or any timezone offset.
			Do not convert the local datetime to UTC.
			If no deadline was requested, return null.
			Never include a completeTask field.

			For UPDATE_TASK and COMPLETE_TASK, include taskId only
			when it is explicitly available in the user's request.
			Do not invent missing task details.

			For NONE, return:
			{"actionType":"NONE","payload":{}}

			Example CREATE_TASK response:
			{"actionType":"CREATE_TASK","payload":{"title":"Example task","description":null,"priority":"MEDIUM","dueAtLocal":"2026-10-21T09:00:00"}}

			Examples are illustrative. Never copy example values unless
			they match the actual user request.
			""")
	String classify(@UserMessage String userMessage);
}
