package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Junaid AI Assistant", appName)
    }

    @Test
    fun `test inserting and querying conversation in Room`() = runBlocking {
        val dao = database.chatDao()
        val conv = ConversationEntity(
            title = "Test AI Session",
            preview = "Hello Junaid AI",
            language = "en"
        )
        dao.insertConversation(conv)

        val list = dao.getAllConversations().first()
        assertEquals(1, list.size)
        assertEquals("Test AI Session", list[0].title)

        val message = ChatMessageEntity(
            conversationId = conv.id,
            role = "user",
            content = "How are you?"
        )
        dao.insertMessage(message)

        val messages = dao.getMessagesForConversation(conv.id).first()
        assertEquals(1, messages.size)
        assertEquals("How are you?", messages[0].content)

        dao.deleteConversation(conv.id)
        val afterDelete = dao.getAllConversations().first()
        assertEquals(0, afterDelete.size)
    }
}
